package sclr.core.actors

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.event.LoggingAdapter
import akka.pattern.pipe
import akka.stream._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, StreamRefs}
import sclr.core.Messages._
import sclr.core.actors.LifecycleActor._
import sclr.core.database.{DatabaseDao, Result}
import sclr.core.strategy.{KDNFStrategy, L2Norm, SupNorm}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class ComputeActor(dao: DatabaseDao, parallelization: Int, computeCountOption: Option[Int]) extends Actor with ActorLogging {
  import context._
  private final case object TrySubscribe
  private final case object Reset
  implicit val mat = ActorMaterializer()(context)

  override def preStart(): Unit = {
    context.system.scheduler.scheduleOnce(delay = 500 millis, self, TrySubscribe)
  }

  def receive: Receive = subscribingToTopic

  private var subscribeOnce: Cancellable = _
  def subscribingToTopic: Receive = {
    case TrySubscribe =>
      DistributedPubSub(context.system).mediator ! Subscribe(workloadTopic, self)
      subscribeOnce = context.system.scheduler.scheduleOnce(delay = 5 seconds, self, TrySubscribe)

    case SubscribeAck(Subscribe(`workloadTopic`, None, `self`)) =>
      if (subscribeOnce != null) subscribeOnce.cancel()
      log.debug(s"ComputeActor - subscribed to topic: $workloadTopic")
      context.become(waitingForWorkload)
  }

  private var workloadId = 0
  def waitingForWorkload: Receive = {
    case activeWorkload: ActiveWorkload =>
      if (activeWorkload.id != workloadId) {
        workloadId = activeWorkload.id
        val manageActor = sender()
        log.info(s"ComputeActor - received workload: $activeWorkload")
        handleWorkload(activeWorkload.workload, manageActor)
      }
  }

  def done: Receive = {
    case Reset =>
      context.become(waitingForWorkload)
  }

  private def handleWorkload(workload: Workload, manageActor: ActorRef): Unit = {
    Try(DatabaseDao.makeSingleTransactor()) match {
      case Success(xa) =>
        val dataset = dao.getDataset(xa, workload.name)
        val strategy: KDNFStrategy = if (workload.useLPNorm) new L2Norm(dataset, workload) else new SupNorm(dataset, workload)

        // obtain the flow you want to attach:
        val flowComputeCount = computeCountOption.map { c => Math.ceil(c / parallelization).toInt }
        log.info(s"ComputeActor - creating $parallelization compute actors")
        for (i <- 0 until parallelization) {
          val computeFlow = ComputeActor.createComputeFlow(strategy, log)

          // Create source of an eventual SinkRef[Work]
          val pullWorkSource: Source[Work, Future[SinkRef[Work]]] = StreamRefs.sinkRef[Work].named(name = "StreamRef-sink-pullWork")
          // Create sink of an eventual SourceRef[Result]
          val pushResultSink: Sink[Result, Future[SourceRef[Result]]] = StreamRefs.sourceRef[Result].named(name = "StreamRef-source-pushResults")
          // materialize both SourceRef and SinkRef (the remote is a source of data, and a sink of data for us):
          val ((pullWorkFuture, futureDone), pushResultFuture) = pullWorkSource
            .viaMat(computeFlow)(Keep.left)
            .watchTermination()(Keep.both)
            .toMat(pushResultSink)(Keep.both)
            .run()

          // wrap the Refs in some domain message
          val reply = for (pullWork <- pullWorkFuture; pushResult <- pushResultFuture) yield {
            system.eventStream.publish(ComputeStreamStarted(self, i))
            WorkComputeReady(pullWork, pushResult, flowComputeCount)
          }

          // When we terminate, send a message to the LifecycleActor.
          futureDone.onComplete {
            case Success(Done) =>
              system.eventStream.publish(ComputeStreamCompleted(self, i))
              self ! Reset
            case Failure(ex) =>
              system.eventStream.publish(ComputeStreamFailed(self, i, ex))
              self ! Reset
          }

          // reply to sender
          reply pipeTo manageActor
        }
        system.eventStream.publish(ComputeActorDone(self))
        context.become(done)

      case Failure(ex) =>
        // we simply wait for the ManageActor to re-send the workload since it goes out repeatedly
        log.error(ex, message = "Could not connect to database.")
    }
  }
}

object ComputeActor {
  def props(dao: DatabaseDao, parallel: Int, computeCountOption: Option[Int] = None) =
    Props(new ComputeActor(dao, parallel, computeCountOption))

  private def createComputeFlow(strategy: KDNFStrategy, log: LoggingAdapter) = {
    Flow[Work].map { work =>
      log.debug(s"ComputeActor - received work: $work")
      strategy.run(work)
    }.named("Compute-flow")
  }
}
