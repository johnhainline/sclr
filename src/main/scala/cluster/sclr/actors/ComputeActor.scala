package cluster.sclr.actors

import akka.Done
import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingAdapter
import akka.pattern.pipe
import akka.stream.scaladsl.{Sink, StreamRefs}
import akka.stream.{ActorMaterializer, SinkRef}
import cluster.sclr.Messages._
import cluster.sclr.core.DatabaseDao
import cluster.sclr.core.strategy.{KDNFStrategy, L2Norm, SupNorm}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class ComputeActor(parallelization: Int, dao: DatabaseDao) extends Actor with ActorLogging {
  import context.dispatcher
  implicit val mat = ActorMaterializer()(context)

  private var sinks: List[Sink[Work, Future[Done]]] = _

  def receive: Receive = {
    case workload: Workload =>
      val dataset = dao.getDataset(workload.name)
      val strategy: KDNFStrategy = if (workload.useLPNorm) new L2Norm(dataset, workload) else new SupNorm(dataset, workload)
      log.debug(s"received workload: $workload")

      // obtain the source you want to offer:
      sinks = (for (i <- 0 until parallelization) yield ComputeActor.createComputeSink(workload, strategy, dao, log)).toList
      for (sink <- sinks) {
        // materialize the SinkRef (the remote is like a source of data for us):
        val ref: Future[SinkRef[Work]] = StreamRefs.sinkRef[Work]().to(sink).run()
        // wrap the SinkRef in some domain message, such that the sender knows what source it is
        val reply: Future[WorkSinkReady] = ref.map(WorkSinkReady)
        // reply to sender
        reply pipeTo sender()
      }
  }
}

object ComputeActor {
  def props(parallelization: Int, dao: DatabaseDao) = Props(new ComputeActor(parallelization, dao))

  private def createComputeSink(workload: Workload, strategy: KDNFStrategy, dao: DatabaseDao, log: LoggingAdapter): Sink[Work, Future[Done]] = Sink.foreach[Work] { work =>
    log.info(s"workload: ${workload.name} received work: $work")
    Try {
      strategy.run(work.selectedDimensions, work.selectedRows).map { result =>
        val rows = dao.insertResult(schema = workload.name, result)
        log.info(s"workload: ${workload.name} saved: $result")
        rows
      }
    } match {
      case Failure(e) =>
        log.error(s"failed to compute work:$work", e)
      case _ =>
    }
  }
}
