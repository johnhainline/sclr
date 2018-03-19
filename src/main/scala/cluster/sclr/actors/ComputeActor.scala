package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._
import cluster.sclr.core.{DatabaseDao, WorkloadRunner}

import scala.util.{Failure, Try}

class ComputeActor(dao: DatabaseDao) extends Actor with ActorLogging {

  private var runner: WorkloadRunner = _
  private var workload: Workload = _
  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topicComputer, self)

  private def askForWork(): Unit = {
    mediator ! Publish(topicManager, GetWork)
  }

  def waiting: Receive = {
    case config: Workload =>
      workload = config
      runner = new WorkloadRunner(dao.getDataset(workload.name), config.useLPNorm)
      log.debug(s"received workload: $config")
      context.become(computing)
      askForWork()
  }

  def computing: Receive = {
    case (work: Work) =>
      log.info(s"workload: ${workload.name} received work: $work")
      Try {
        runner.run(workload.dnfSize, workload.mu, work.selectedDimensions, work.selectedRows).map { result =>
          val rows = dao.insertResult(schema = workload.name, result)
          log.info(s"workload: ${workload.name} saved: $result")
          rows
        }
      } match {
        case Failure(e) =>
          log.error(s"failed to compute work:$work", e)
        case _ =>
      }
      askForWork()
    case Finished =>
      log.debug("received finished")
      context.become(finished)
  }

  def finished: Receive = {
    case _ => Unit
  }

  def receive: Receive = waiting

}

object ComputeActor {
  def props(resultsDao: DatabaseDao) = Props(new ComputeActor(resultsDao))
}
