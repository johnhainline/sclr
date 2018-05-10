package cluster.sclr.actors

import akka.Done
import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.event.LoggingAdapter
import akka.pattern.pipe
import akka.stream.scaladsl.{Sink, StreamRefs}
import akka.stream.{ActorMaterializer, SinkRef}
import cluster.sclr.Messages._
import cluster.sclr.core.DatabaseDao
import cluster.sclr.core.strategy.{KDNFStrategy, L2Norm, SupNorm}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Try}

class ComputeActor(parallelization: Int, dao: DatabaseDao) extends Actor with ActorLogging {
  import context._
  final case object TrySubscribe
  implicit val mat = ActorMaterializer()(context)
  private var sinks: List[Sink[Work, Future[Done]]] = _

  override def preStart() = {
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

      // obtain the source you want to offer:
      sinks = (for (i <- 0 until parallelization) yield ComputeActor.createComputeSink(workload, strategy, dao, log)).toList
      for (sink <- sinks) {
        // materialize the SinkRef (the remote is like a source of data for us):
        val ref: Future[SinkRef[Work]] = StreamRefs.sinkRef[Work]().to(sink).run()
        // wrap the SinkRef in some domain message, such that the sender knows what source it is
        val reply: Future[WorkSinkReady] = ref.map(WorkSinkReady)
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
    log.error(s"ComputeActor - stopped!")
  }
}

object ComputeActor {
  def props(parallelization: Int, dao: DatabaseDao) = Props(new ComputeActor(parallelization, dao))

  private def createComputeSink(workload: Workload, strategy: KDNFStrategy, dao: DatabaseDao, log: LoggingAdapter): Sink[Work, Future[Done]] = Sink.foreach[Work] { work =>
    log.info(s"ComputeActor - workload: ${workload.name} received work: $work")
    Try {
      strategy.run(work.selectedDimensions, work.selectedRows).map { result =>
        val rows = dao.insertResult(schema = workload.name, result)
        log.info(s"ComputeActor - workload: ${workload.name} saved: $result")
        rows
      }
    } match {
      case Failure(e) =>
        log.error(s"ComputeActor - failed to compute work:$work", e)
      case _ =>
    }
  }
}
