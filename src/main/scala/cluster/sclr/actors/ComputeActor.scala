package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.event.LoggingAdapter
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.updated.stream.{SinkRef, SourceRef}
import akka.updated.stream.scaladsl.StreamRefs
import cluster.sclr.Messages._
import cluster.sclr.core.strategy.{KDNFStrategy, L2Norm, SupNorm}
import cluster.sclr.core.{DatabaseDao, Result}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class ComputeActor(parallelization: Int, dao: DatabaseDao) extends Actor with ActorLogging {
  import context._
  final case object TrySubscribe
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

  def waitingForWorkload: Receive = {
    case workload: Workload =>
      val manageActor = sender()
      log.debug(s"ComputeActor - received workload: $workload")
      val dataset = dao.getDataset(workload.name)
      val strategy: KDNFStrategy = if (workload.useLPNorm) new L2Norm(dataset, workload) else new SupNorm(dataset, workload)

      // obtain the flow you want to attach:
      val flows = (for (i <- 0 until parallelization) yield ComputeActor.createComputeFlow(strategy, log)).toList
      for (flow <- flows) {
        // Create source of an eventual SinkRef[Work]
        val pullWorkSource: Source[Work, Future[SinkRef[Work]]] = StreamRefs.sinkRef[Work]()
        // Create sink of an eventual SourceRef[Result]
        val pushResultSink: Sink[Result, Future[SourceRef[Result]]] = StreamRefs.sourceRef[Result]()
        // materialize both SourceRef and SinkRef (the remote is a source of data, and a sink of data for us):
        val ref = pullWorkSource.viaMat(flow)(Keep.left).toMat(pushResultSink)(Keep.both).run()
        // wrap the Refs in some domain message
        val reply = for (pullWork <- ref._1; pushResult <- ref._2) yield {
          WorkComputeReady(pullWork, pushResult)
        }
        // reply to sender
        reply pipeTo manageActor
      }
      context.become(done)
  }

  def done: Receive = {
    case _ => Unit
  }

  override def postStop(): Unit = {
    super.postStop()
  }
}

object ComputeActor {
  def props(parallelization: Int, dao: DatabaseDao) = Props(new ComputeActor(parallelization, dao))

  private def createComputeFlow(strategy: KDNFStrategy, log: LoggingAdapter) = Flow[Work].map { work =>
    log.info(s"ComputeActor - received work: $work")
    strategy.run(work.selectedDimensions, work.selectedRows)
  }
}
