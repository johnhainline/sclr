package sclr.core.actors

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.remote.Ack
import akka.stream.contrib.Retry
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl._
import cats.effect.IO
import sclr.core.Messages._
import sclr.core.database.{DatabaseDao, DatasetInfo, Result}
import sclr.core.http.InfoService
import combinations.Combinations
import combinations.iterators.MultipliedIterator
import doobie.util.transactor.Transactor
import sclr.core.actors.ManageActor.DB_CONNECTIONS
import streams.BalanceHub

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Random, Success, Try}

class ManageActor(infoService: InfoService, dao: DatabaseDao, r: Random = new Random()) extends Actor with ActorLogging {
  private case object SendWorkload
  import context._

  infoService.setManageActor(self)
  implicit val mat = ActorMaterializer()(context)

  def receive: Receive = waitingForWorkload

  def waitingForWorkload: Receive = {
    case workload: Workload =>
      val xa = DatabaseDao.makeHikariTransactor(ManageActor.DB_CONNECTIONS)
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

      val makeConnection = makeConnectionFunctionSimple(xa, dao, info, workload, r)

      self ! SendWorkload
      context.become(sendingWorkload(workload, makeConnection))
  }

  def sendingWorkload(workload: Workload, makeConnection: WorkComputeReady => Unit)(): Receive = {
    case SendWorkload =>
      log.debug(s"ManageActor - sending workload: $workload to topic: $workloadTopic")
      DistributedPubSub(context.system).mediator ! Publish(workloadTopic, workload)
      context.system.scheduler.scheduleOnce(delay = 1 seconds, self, SendWorkload)
    case workComputeReady: WorkComputeReady =>
      log.debug(s"ManageActor - received $workComputeReady")
      makeConnection(workComputeReady)
  }

  private def makeConnectionFunctionSimple(xa: Transactor[IO], dao: DatabaseDao, info: DatasetInfo, workload: Workload, r: Random): WorkComputeReady => Unit = {
    // An iterator that runs through (ySize choose 2) * (rows choose 2)
    val iteratorGen = () => ManageActor.createIterator(rowCount = info.rowCount, yLength = info.yLength, rowsConstant = workload.getRowsConstant(), workload.optionalSubset, r)

    // Materializing these objects lets us eventually generate SourceRef/SinkRef instances to bind to ComputeActor.
    val hubFlow = Flow.fromSinkAndSourceCoupledMat(BalanceHub.sink[Work](), MergeHub.source[Result])(Keep.both)

    val entireStream = RunnableGraph.fromGraph(GraphDSL.create(hubFlow) { implicit b => implicit hub =>
      import GraphDSL.Implicits._

      val source  = Source.fromIterator(iteratorGen)
      val groupRs = b.add(Flow[Result].groupedWithin(50, 100 milliseconds))
      val save    = Sink.foreachParallel(parallelism = DB_CONNECTIONS) {dao.insertResults(xa, workload.name)}

      source ~> hub ~> groupRs ~> save
      ClosedShape
    })

    // Materialize the stream, getting back our endlessly materializable source and sink from BalanceHub and MergeHub.
    val (pullWorkSource, pushResultsSink) = entireStream.run()

    val makeConnection: WorkComputeReady => Unit = (workComputeReady: WorkComputeReady) => {
      val computeFlow = Flow.fromSinkAndSourceCoupled(workComputeReady.pullWork, workComputeReady.pushResult)
      pullWorkSource.via(computeFlow).to(pushResultsSink).run()
    }
    makeConnection
  }

  private def makeConnectionFunctionOuterRetry(xa: Transactor[IO], dao: DatabaseDao, info: DatasetInfo, workload: Workload, r: Random): WorkComputeReady => Unit = {
    def partitionFunction(tuple: (Try[Result], Work)): Int = {
      tuple match {
        case (Success(_), _) => 0
        case (Failure(_), _) => 1
        case _ => throw new RuntimeException(s"Received invalid entry $tuple")
      }
    }

    // An iterator that runs through (ySize choose 2) * (rows choose 2)
    val iteratorGen = () => ManageActor.createIterator(rowCount = info.rowCount, yLength = info.yLength, rowsConstant = workload.getRowsConstant(), workload.optionalSubset, r)

    // Materializing these objects lets us eventually generate SourceRef/SinkRef instances to bind to ComputeActor.
    val hubFlow = Flow.fromSinkAndSourceCoupledMat(BalanceHub.sink[Work](), MergeHub.source[Result])(Keep.both)
      .map[Try[Result]](Success(_))//.recover{ case e: Exception => Failure(e)}

    val entireStream = RunnableGraph.fromGraph(GraphDSL.create(hubFlow) { implicit b => implicit hub =>
      import GraphDSL.Implicits._

      val source  = Source.fromIterator(iteratorGen)
      val merge   = b.add(MergePreferred[Work](secondaryPorts = 1, eagerComplete = false))
      val bcast   = b.add(Broadcast[Work](outputPorts = 2, eagerCancel = false))
      val zip     = b.add(Zip[Try[Result], Work]())
      val part    = b.add(Partition[(Try[Result], Work)](outputPorts = 2, partitioner = partitionFunction))
      val getR    = b.add(Flow[(Try[Result], Work)].map(_._1.get))
      val getW    = b.add(Flow[(Try[Result], Work)].map(_._2))
      val groupRs = b.add(Flow[Result].groupedWithin(50, 100 milliseconds))
      val save    = Sink.foreachParallel(parallelism = DB_CONNECTIONS) {dao.insertResults(xa, workload.name)}

      source ~> merge.in(0)
      merge.out ~> bcast ~> hub ~> zip.in0
      bcast        ~> zip.in1
      zip.out ~> part.in
      part.out(0) ~> getR ~> groupRs ~> save
      part.out(1) ~> getW ~> merge.preferred
      ClosedShape
    })

    // Materialize the stream, getting back our endlessly materializable source and sink from BalanceHub and MergeHub.
    val (pullWorkSource, pushResultsSink) = entireStream.run()
    val makeConnection: WorkComputeReady => Unit = (workComputeReady: WorkComputeReady) => {
      val computeFlow = Flow.fromSinkAndSourceCoupled(workComputeReady.pullWork, workComputeReady.pushResult)
      pullWorkSource.via(computeFlow).to(pushResultsSink).run()
    }
    makeConnection
  }

  private def makeConnectionFunctionInnerRetry(xa: Transactor[IO], dao: DatabaseDao, info: DatasetInfo, workload: Workload, r: Random): WorkComputeReady => Unit = {
    // An iterator that runs through (ySize choose 2) * (rows choose 2)
    val iteratorGen = () => ManageActor.createIterator(rowCount = info.rowCount, yLength = info.yLength, rowsConstant = workload.getRowsConstant(), workload.optionalSubset, r)

    val workSource = Source.fromIterator(iteratorGen)
    val saveResultsSink = Sink.foreachParallel(parallelism = DB_CONNECTIONS) {
      dao.insertResults(xa, workload.name)
    }

    val hubFlow = Flow.fromSinkAndSourceCoupledMat(BalanceHub.sink[(Work, Work)](), MergeHub.source[(Try[Result], Work)])(Keep.both)

    // Always retry failed work.
    val retryFlow = Retry(hubFlow) { work => Some((work, work)) }

    // Construct workSource -> Work = (Work, Work) -> Retry(BalanceHub sink -> MergeHub source) -> (Try[Result], Work) = Result -> chunk Results -> save Results
    val expand = Flow[Work].map(work => (work, work))
    val flatten = Flow[(Try[Result], Work)].map(tuple => tuple._1.get)
    val entireStream = workSource.via(expand).viaMat(retryFlow)(Keep.right).via(flatten).groupedWithin(50, 100 milliseconds).to(saveResultsSink)

    // Materialize the stream, getting back our endlessly materializable source and sink from BalanceHub and MergeHub.
    val (pullWorkSource, pushResultsSink) = entireStream.run()

    // Construct an anonymous method that creates our connection to a WorkComputeReady instance.
    val makeConnection: WorkComputeReady => Unit = (workComputeReady: WorkComputeReady) => {
      val computeFlow = Flow.fromSinkAndSourceCoupled(workComputeReady.pullWork, workComputeReady.pushResult)
        .map[Try[Result]](Success(_)).recover { case e: Exception => Failure(e) }

      val stream = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._
        val in = builder.add(Flow[(Work, Work)].map(_._1)) // (Work, Work) => Work
      val broadcast = builder.add(Broadcast[Work](outputPorts = 2, eagerCancel = true))
        val zip = builder.add(Zip[Try[Result], Work]())
        pullWorkSource ~> in ~> broadcast ~> computeFlow ~> zip.in0
        broadcast ~> zip.in1
        zip.out ~> pushResultsSink
        ClosedShape
      })
      stream.run()
    }
    makeConnection
  }

}

object ManageActor {
  private val DB_CONNECTIONS = 3
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
