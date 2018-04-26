package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._
import cluster.sclr.core.strategy.{KDNFStrategy, L2Norm, SupNorm}
import cluster.sclr.core.{DatabaseDao, Dataset}

import scala.util.{Failure, Try}

class ComputeActor(dao: DatabaseDao) extends Actor with ActorLogging {

  private var strategy: KDNFStrategy = _
  private var dataset: Dataset = _
  private var workload: Workload = _
  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topicComputer, self)

  private def askForWork(): Unit = {
    mediator ! Publish(topicManager, GetWork)
  }

  def init: Receive = {
    case SubscribeAck(Subscribe("content", None, `self`)) =>
      log.debug(s"subscribed to topic: $topicComputer")
      context.become(waiting)
  }

  def waiting: Receive = {
    case config: Workload =>
      workload = config
      dataset = dao.getDataset(config.name)
      strategy = if (config.useLPNorm) new L2Norm(dataset, config) else new SupNorm(dataset, config)
      log.debug(s"received workload: $config")
      context.become(computing)
      askForWork()
  }

  def computing: Receive = {
    case (work: Work) =>
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
