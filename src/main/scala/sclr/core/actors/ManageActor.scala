package sclr.core.actors

import akka.Done
import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.remote.Ack
import akka.stream.scaladsl._
import akka.stream.ActorMaterializer
import cats.effect.IO
import combinations.Combinations
import combinations.iterators.MultipliedIterator
import doobie.util.transactor.Transactor
import sclr.core.Messages._
import sclr.core.actors.LifecycleActor.{ManageStreamCompleted, ManageStreamFailed, ManageStreamStarted}
import sclr.core.actors.ManageActor.DB_CONNECTIONS
import sclr.core.database.{DatabaseDao, DatasetInfo, Result}
import sclr.core.http.SclrService
import streams.{BalanceHub, MergeHub}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Random, Success, Try}

class ManageActor(infoService: SclrService, dao: DatabaseDao, workloadOption: Option[Workload]) extends Actor with ActorLogging {
  import context._
  private case object SendActiveWorkload
  private case object Reset
  infoService.setManageActor(self)
  implicit val mat = ActorMaterializer()(context)

  override def preStart(): Unit = {
    // If we have an existing workload, then send it to ourselves so we start running it.
    workloadOption.foreach { workload =>
      context.system.scheduler.scheduleOnce(delay = 500 millis, self, workload)
    }
  }

  def receive: Receive = waitingForWorkload

  def waitingForWorkload: Receive = {
    case workload: Workload =>
      log.info(s"ManageActor - received workload: $workload")
      sender() ! Ack

      Try(DatabaseDao.makeHikariTransactor(ManageActor.DB_CONNECTIONS)) match {
        case Success(xa) =>
          dao.clearDataset(xa, workload.name)
          dao.initializeDataset(xa, workload.name)
          dao.setupSchemaAndTable(xa, workload.name, ManageActor.Y_DIMENSIONS, workload.getRowsConstant())
          context.become(prepareWorkload(xa))
          self ! workload
        case Failure(ex) =>
          log.error(ex, message = "Could not connect to database.")
          context.system.scheduler.scheduleOnce(delay = 5 seconds, self, workload)
      }
  }

  private var workloadId = 1
  def prepareWorkload(xa: Transactor[IO])(): Receive = {
    case workload: Workload =>
      val info = dao.getDatasetInfo(xa, workload.name)
      val size = workload.optionalSubset.getOrElse(info.rowCount)
      val terms = info.xLength * workload.dnfSize * 4
      val rows = workload.getRowsConstant()
      val sizeYDim = Combinations(info.yLength, workload.dnfSize).size
      val sizeRows = Combinations(size, rows).size
      val total = sizeYDim * sizeRows
      log.info(s"ManageActor - dataset: ${workload.name}")
      log.info(s"size: $size xLength: ${info.xLength}, yLength: ${info.yLength}")
      log.info(s"terms (${info.xLength} * ${workload.dnfSize} * 4): $terms")
      log.info(s"yDim combos (${info.yLength} choose 2): $sizeYDim")
      log.info(s"row combos ($size choose $rows): $sizeRows")
      log.info(s"$total items, $terms terms")

      val makeConnection = makeConnectionFunctionSimple(xa, dao, info, workload)

      system.eventStream.publish(ManageStreamStarted(self, workload))
      self ! SendActiveWorkload
      context.become(sendingWorkload(ActiveWorkload(workloadId, workload), makeConnection))
      workloadId += 1
  }

  private var sendSchedule: Option[Cancellable] = None
  def sendingWorkload(activeWorkload: ActiveWorkload, makeConnection: WorkComputeReady => Unit)(): Receive = {
    case SendActiveWorkload =>
      log.debug(s"ManageActor - sending workload: $activeWorkload to topic: $workloadTopic")
      DistributedPubSub(context.system).mediator ! Publish(workloadTopic, activeWorkload)
      sendSchedule = Some(context.system.scheduler.scheduleOnce(delay = 5 seconds, self, SendActiveWorkload))
    case workComputeReady: WorkComputeReady =>
      log.debug(s"ManageActor - received $workComputeReady")
      makeConnection(workComputeReady)
    case Reset =>
      log.info(s"ManageActor - finished workload, resetting...")
      sendSchedule.map(_.cancel())
      context.become(waitingForWorkload)
  }

  private def makeConnectionFunctionSimple(xa: Transactor[IO], dao: DatabaseDao, info: DatasetInfo, workload: Workload): WorkComputeReady => Unit = {
    // An iterator that runs through (ySize choose 2) * (rows choose 2)
    val iteratorGen = () => ManageActor.createIterator(rowCount = info.rowCount, yLength = info.yLength, rowsConstant = workload.getRowsConstant(), workload.optionalSubset, workload.optionalRandomSeed)

    // Materializing these objects lets us eventually generate SourceRef/SinkRef instances to bind to ComputeActor.
    val balanceHub = BalanceHub.sink[Work](bufferSize = 32)
    val mergeHub = MergeHub.source[Result](perProducerBufferSize = 16, completeWhenAllUpstreamsComplete = true)
    val hubFlow = Flow.fromSinkAndSourceMat(balanceHub, mergeHub)(Keep.both)

    val source  = Source.fromIterator(iteratorGen)
    val bufferSize = system.settings.config.getInt("akka.stream.materializer.stream-ref.buffer-capacity")
    val groupRs = Flow[Result].groupedWithin(bufferSize * DB_CONNECTIONS, 1000 milliseconds)
    val save    = Sink.foreachParallel(parallelism = DB_CONNECTIONS) {dao.insertResults(xa, workload.name)}

    // Materialize the stream, getting back our endlessly materializable source and sink from BalanceHub and MergeHub.
    val ((pullWorkSource, pushResultsSink), sinkDone) =
      source
      .viaMat(hubFlow)(Keep.right)
      .viaMat(groupRs)(Keep.left)
      .toMat(save)(Keep.both)
      .run()

    // When we terminate, send a message to the LifecycleActor.
    sinkDone.onComplete {
      case Success(Done) =>
        system.eventStream.publish(ManageStreamCompleted(self, workload))
        self ! Reset
      case Failure(ex) =>
        system.eventStream.publish(ManageStreamFailed(self, workload, ex))
        self ! Reset
    }

    val makeConnection: WorkComputeReady => Unit = (workComputeReady: WorkComputeReady) => {
      val computeFlow = Flow.fromSinkAndSource(workComputeReady.pullWork, workComputeReady.pushResult)
      val derivedSource = if (workComputeReady.computeCountOption.nonEmpty) {
        log.debug(s"ManageActor - linked compute node only pulling work ${workComputeReady.computeCountOption.get} times")
        pullWorkSource.take(workComputeReady.computeCountOption.get)
      } else {
        pullWorkSource
      }
      derivedSource.via(computeFlow).toMat(pushResultsSink)(Keep.both).run()
    }
    makeConnection
  }

//  private def makeConnectionFunctionOuterRetry(xa: Transactor[IO], dao: DatabaseDao, info: DatasetInfo, workload: Workload, r: Random): WorkComputeReady => Unit = {
//    def partitionFunction(tuple: (Try[Result], Work)): Int = {
//      tuple match {
//        case (Success(_), _) => 0
//        case (Failure(_), _) => 1
//        case _ => throw new RuntimeException(s"Received invalid entry $tuple")
//      }
//    }
//
//    // An iterator that runs through (ySize choose 2) * (rows choose 2)
//    val iteratorGen = () => ManageActor.createIterator(rowCount = info.rowCount, yLength = info.yLength, rowsConstant = workload.getRowsConstant(), workload.optionalSubset, r)
//
//    // Materializing these objects lets us eventually generate SourceRef/SinkRef instances to bind to ComputeActor.
//    val hubFlow = Flow.fromSinkAndSourceCoupledMat(BalanceHub.sink[Work](), MergeHub.source[Result])(Keep.both)
//      .map[Try[Result]](Success(_))//.recover{ case e: Exception => Failure(e)}
//
//    val entireStream = RunnableGraph.fromGraph(GraphDSL.create(hubFlow) { implicit b => implicit hub =>
//      import GraphDSL.Implicits._
//
//      val source  = Source.fromIterator(iteratorGen)
//      val merge   = b.add(MergePreferred[Work](secondaryPorts = 1, eagerComplete = false))
//      val bcast   = b.add(Broadcast[Work](outputPorts = 2, eagerCancel = false))
//      val zip     = b.add(Zip[Try[Result], Work]())
//      val part    = b.add(Partition[(Try[Result], Work)](outputPorts = 2, partitioner = partitionFunction))
//      val getR    = b.add(Flow[(Try[Result], Work)].map(_._1.get))
//      val getW    = b.add(Flow[(Try[Result], Work)].map(_._2))
//      val groupRs = b.add(Flow[Result].groupedWithin(50, 100 milliseconds))
//      val save    = Sink.foreachParallel(parallelism = DB_CONNECTIONS) {dao.insertResults(xa, workload.name)}
//
//      source ~> merge.in(0)
//      merge.out ~> bcast ~> hub ~> zip.in0
//      bcast        ~> zip.in1
//      zip.out ~> part.in
//      part.out(0) ~> getR ~> groupRs ~> save
//      part.out(1) ~> getW ~> merge.preferred
//      ClosedShape
//    })
//
//    // Materialize the stream, getting back our endlessly materializable source and sink from BalanceHub and MergeHub.
//    val (pullWorkSource, pushResultsSink) = entireStream.run()
//    val makeConnection: WorkComputeReady => Unit = (workComputeReady: WorkComputeReady) => {
//      val computeFlow = Flow.fromSinkAndSourceCoupled(workComputeReady.pullWork, workComputeReady.pushResult)
//      pullWorkSource.via(computeFlow).to(pushResultsSink).run()
//    }
//    makeConnection
//  }
//
//  private def makeConnectionFunctionInnerRetry(xa: Transactor[IO], dao: DatabaseDao, info: DatasetInfo, workload: Workload, r: Random): WorkComputeReady => Unit = {
//    // An iterator that runs through (ySize choose 2) * (rows choose 2)
//    val iteratorGen = () => ManageActor.createIterator(rowCount = info.rowCount, yLength = info.yLength, rowsConstant = workload.getRowsConstant(), workload.optionalSubset, r)
//
//    val workSource = Source.fromIterator(iteratorGen)
//    val saveResultsSink = Sink.foreachParallel(parallelism = DB_CONNECTIONS) {
//      dao.insertResults(xa, workload.name)
//    }
//
//    val hubFlow = Flow.fromSinkAndSourceCoupledMat(BalanceHub.sink[(Work, Work)](), MergeHub.source[(Try[Result], Work)])(Keep.both)
//
//    // Always retry failed work.
//    val retryFlow = Retry(hubFlow) { work => Some((work, work)) }
//
//    // Construct workSource -> Work = (Work, Work) -> Retry(BalanceHub sink -> MergeHub source) -> (Try[Result], Work) = Result -> chunk Results -> save Results
//    val expand = Flow[Work].map(work => (work, work))
//    val flatten = Flow[(Try[Result], Work)].map(tuple => tuple._1.get)
//    val entireStream = workSource.via(expand).viaMat(retryFlow)(Keep.right).via(flatten).groupedWithin(50, 100 milliseconds).to(saveResultsSink)
//
//    // Materialize the stream, getting back our endlessly materializable source and sink from BalanceHub and MergeHub.
//    val (pullWorkSource, pushResultsSink) = entireStream.run()
//
//    // Construct an anonymous method that creates our connection to a WorkComputeReady instance.
//    val makeConnection: WorkComputeReady => Unit = (workComputeReady: WorkComputeReady) => {
//      val computeFlow = Flow.fromSinkAndSourceCoupled(workComputeReady.pullWork, workComputeReady.pushResult)
//        .map[Try[Result]](Success(_)).recover { case e: Exception => Failure(e) }
//
//      val stream = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
//        import GraphDSL.Implicits._
//        val in = builder.add(Flow[(Work, Work)].map(_._1)) // (Work, Work) => Work
//      val broadcast = builder.add(Broadcast[Work](outputPorts = 2, eagerCancel = true))
//        val zip = builder.add(Zip[Try[Result], Work]())
//        pullWorkSource ~> in ~> broadcast ~> computeFlow ~> zip.in0
//        broadcast ~> zip.in1
//        zip.out ~> pushResultsSink
//        ClosedShape
//      })
//      stream.run()
//    }
//    makeConnection
//  }
}

object ManageActor {
  private val DB_CONNECTIONS = 3
  private val Y_DIMENSIONS = 2

  def props(infoService: SclrService, resultsDao: DatabaseDao, workloadOption: Option[Workload] = None) =
    Props(new ManageActor(infoService, resultsDao, workloadOption))

  def createIterator(rowCount: Int, yLength: Int, rowsConstant: Int, optionalSubset: Option[Int], optionalRandomSeed: Option[Int]): Iterator[Work] = {
    val selectYDimensions = () => Combinations(yLength, ManageActor.Y_DIMENSIONS).iterator()
    val selectRows = if (optionalSubset.isEmpty) {
      () => Combinations(rowCount, rowsConstant).iterator()
    } else {
      val seed = optionalRandomSeed.getOrElse(Random.nextInt())
      () => Combinations(rowCount, rowsConstant).subsetIterator(optionalSubset.get, new Random(seed))
    }
    val iterator = MultipliedIterator(Vector(selectYDimensions, selectRows)).zipWithIndex.map { case (next, index) =>
      Work(index, selectedDimensions = next.head, selectedRows = next.last)
    }
    iterator
  }
}
