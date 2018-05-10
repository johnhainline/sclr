package cluster.sclr.actors

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.remote.Ack
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BalanceHub, Keep, Source}
import cluster.sclr.Messages._
import cluster.sclr.core.DatabaseDao
import cluster.sclr.http.InfoService
import combinations.Combinations
import combinations.iterators.MultipliedIterator

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class ManageActor(infoService: InfoService, dao: DatabaseDao, r: Random = new Random()) extends Actor with ActorLogging {
  private case object SendWorkload
  import context._

  infoService.setManageActor(self)
  implicit val mat = ActorMaterializer()(context)

  def receive: Receive = waitingForWorkload

  def waitingForWorkload: Receive = {
    case workload: Workload =>
      dao.clearDataset(workload.name)
      dao.initializeDataset(workload.name)
      dao.setupSchemaAndTable(workload.name, ManageActor.Y_DIMENSIONS, workload.getRowsConstant())
      log.debug(s"ManageActor - received workload: $workload")
      context.become(prepareWorkload)
      self ! workload
      sender() ! Ack
  }

  def prepareWorkload: Receive = {
    case workload: Workload =>
      val info = dao.getDatasetInfo(workload.name)
      log.debug(s"ManageActor - preparing workload for dataset: ${workload.name} with dimensions:${info.xLength} rows:${info.rowCount} selecting dimensions:${ManageActor.Y_DIMENSIONS} rows:${workload.getRowsConstant()}")
      // An iterator that runs through (ySize choose 2) * (rows choose 2)
      val iteratorGen = () => ManageActor.createIterator(info.rowCount, info.yLength, workload.getRowsConstant(), workload.optionalSubset, r)
      // A simple producer that runs through our iterator
      val producer = Source.fromIterator(iteratorGen)
      // Attach a BalanceHub Sink to the producer to multiplex. This will materialize to a corresponding Source.
      // (We need to use toMat and Keep.right since by default the materialized value to the left is used)
      val runnableGraph = producer.toMat(BalanceHub.sink())(Keep.right)
      // Create the source that we will attach our "compute" sinks to.
      val source = runnableGraph.run()
      self ! SendWorkload
      context.become(sendingWorkload(workload, source))
  }

  def sendingWorkload(workload: Workload, source: Source[Work, NotUsed])(): Receive = {
    case SendWorkload =>
      log.debug(s"ManageActor - sending workload to topic: $workloadTopic")
      DistributedPubSub(context.system).mediator ! Publish(workloadTopic, workload)
      context.system.scheduler.scheduleOnce(delay = 5 seconds, self, SendWorkload)
    case WorkSinkReady(sinkRef) =>
      log.debug(s"ManageActor - received WorkSinkReady($sinkRef)")
      source.runWith(sinkRef)
  }

  override def postStop(): Unit = {
    super.postStop()
    log.error(s"ManageActor - stopped!")
  }
}

object ManageActor {
  private val Y_DIMENSIONS = 2

  def props(infoService: InfoService, resultsDao: DatabaseDao) = Props(new ManageActor(infoService, resultsDao))

  private def createIterator(rowCount: Int, yLength: Int, rowsConstant: Int, optionalSubset: Option[Int], r: Random): Iterator[Work] = {
    val selectYDimensions = () => Combinations(yLength, ManageActor.Y_DIMENSIONS).iterator()
    val selectRows = if (optionalSubset.isEmpty) {
      () => Combinations(rowCount, rowsConstant).iterator()
    } else {
      () => Combinations(rowCount, rowsConstant).subsetIterator(optionalSubset.get, r)
    }
    val iterator = MultipliedIterator(Vector(selectYDimensions, selectRows)).map(
      next => Work(selectedDimensions = next.head, selectedRows = next.last)
    )
    iterator
  }
}
