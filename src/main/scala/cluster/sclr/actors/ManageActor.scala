package cluster.sclr.actors

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.remote.Ack
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import cats.effect.IO
import cluster.sclr.Messages._
import cluster.sclr.database.{DatabaseDao, Result}
import cluster.sclr.http.InfoService
import combinations.Combinations
import combinations.iterators.MultipliedIterator
import doobie.util.transactor.Transactor
import streams.BalanceHub

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class ManageActor(infoService: InfoService, dao: DatabaseDao, r: Random = new Random()) extends Actor with ActorLogging {
  private case object SendWorkload
  import context._

  infoService.setManageActor(self)
  implicit val mat = ActorMaterializer()(context)
  private val databaseConnections = 3

  def receive: Receive = waitingForWorkload

  def waitingForWorkload: Receive = {
    case workload: Workload =>
      val xa = DatabaseDao.makeHikariTransactor(databaseConnections)
      dao.clearDataset(xa, workload.name)
      dao.initializeDataset(xa, workload.name)
      dao.setupSchemaAndTable(xa, workload.name, ManageActor.Y_DIMENSIONS, workload.getRowsConstant())
      log.debug(s"ManageActor - received workload: $workload")
      context.become(prepareWorkload(xa))
      self ! workload
      sender() ! Ack
  }

  def prepareWorkload(xa: Transactor[IO])(): Receive = {
    case workload: Workload =>
      val info = dao.getDatasetInfo(xa, workload.name)
      log.debug(s"ManageActor - preparing workload for dataset: ${workload.name} with dimensions:${info.yLength} rows:${info.rowCount} selecting dimensions:${ManageActor.Y_DIMENSIONS} rows:${workload.getRowsConstant()}")
      // An iterator that runs through (ySize choose 2) * (rows choose 2)
      val iteratorGen = () => ManageActor.createIterator(rowCount = info.rowCount, yLength = info.yLength, rowsConstant = workload.getRowsConstant(), workload.optionalSubset, r)

      // A simple producer that runs through our iterator
      val producer = Source.fromIterator(iteratorGen)

      // Attach a BalanceHub Sink to the producer to multiplex. This will materialize to a corresponding Source.
      // (We need to use toMat and Keep.right since by default the materialized value to the left is used)
      val runnableSourceGraph = producer.toMat(BalanceHub.sink(bufferSize = 16))(Keep.right)
      // Create the source that we will attach our compute "pushWork" sinks to.
      val source = runnableSourceGraph.run()

      // A simple consumer that saves our results
      val consumer = Sink.foreachParallel(parallelism = databaseConnections) {dao.insertResults(xa, workload.name)}

      // MergeHub source de-multiplexes from our pushResultSink.
      val mergeHubSource = MergeHub.source[Result](perProducerBufferSize = 16)
      // We include a groupedWithin to make the database save in batches. We attach to our consumer to actually save.
      val runnableSinkGraph = mergeHubSource.groupedWithin(50, 100 milliseconds).named(name = "DB-grouper").to(consumer)
      val sink = runnableSinkGraph.run()

      self ! SendWorkload
      context.become(sendingWorkload(workload, source, sink))
  }

  // iteratorSource -> pullWorkSource -> computeFlow -> pushResultsSink -> saveSink
  def sendingWorkload(workload: Workload, source: Source[Work, NotUsed], sink: Sink[Result, NotUsed])(): Receive = {
    case SendWorkload =>
      log.debug(s"ManageActor - sending workload: $workload to topic: $workloadTopic")
      DistributedPubSub(context.system).mediator ! Publish(workloadTopic, workload)
      context.system.scheduler.scheduleOnce(delay = 5 seconds, self, SendWorkload)
    case WorkComputeReady(pushWorkSink, pullResultSource) =>
      log.debug(s"ManageActor - received WorkSinkReady($pushWorkSink, $pullResultSource)")
      // compute: flow = pullWorkSource -> computeFlow -> pushResultsSink
      // manage:  flow = pushWorkSink   -> pullResultSource
      val flow = Flow.fromSinkAndSource(pushWorkSink, pullResultSource).named(name = "fromSinkAndSource")
      source.via(flow).runWith(sink)
  }
}

object ManageActor {
  private val Y_DIMENSIONS = 2

  def props(infoService: InfoService, resultsDao: DatabaseDao) = Props(new ManageActor(infoService, resultsDao))

  def createIterator(rowCount: Int, yLength: Int, rowsConstant: Int, optionalSubset: Option[Int], r: Random): Iterator[Work] = {
    val selectYDimensions = () => Combinations(yLength, ManageActor.Y_DIMENSIONS).iterator()
    val selectRows = if (optionalSubset.isEmpty) {
      () => Combinations(rowCount, rowsConstant).iterator()
    } else {
      val iteratorSeed = r.nextLong()
      () => Combinations(rowCount, rowsConstant).subsetIterator(optionalSubset.get, new Random(iteratorSeed))
    }
    val iterator = MultipliedIterator(Vector(selectYDimensions, selectRows)).zipWithIndex.map { case (next, index) =>
      Work(index, selectedDimensions = next.head, selectedRows = next.last)
    }
    iterator
  }
}
