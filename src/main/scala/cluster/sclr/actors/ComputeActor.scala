package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._
import cluster.sclr.core.{DatabaseDao, WorkloadRunner}

import scala.util.{Failure, Try}

class ComputeActor(dao: DatabaseDao) extends Actor with ActorLogging {

  private var runner: WorkloadRunner = _
  private var name: String = _
  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topicComputer, self)

  private def askForWork(): Unit = {
    mediator ! Publish(topicManager, GetWork)
  }

  def waiting: Receive = {
    case workload:Workload =>
      name = workload.name
      val dataset = dao.getDataset(name)
      runner = new WorkloadRunner(dataset.x, dataset.yz)
      log.debug("waiting -> computing")
      context.become(computing)
      askForWork()
  }

  def computing: Receive = {
    case (work: Work) =>
      Try {
        val optionResult = runner.run(work.selectedDimensions, work.selectedRows)
        optionResult.map { result =>
          dao.insertResult(schema = name, result)
        }
      } match {
        case Failure(e) =>
          e.printStackTrace()
        case _ =>
      }
      askForWork()
    case Finished =>
      log.debug("computing -> finished")
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
