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
      runner = new WorkloadRunner(dao.getDataset(workload.name))
      log.debug("waiting -> computing")
      context.become(computing)
      askForWork()
  }

  def computing: Receive = {
    case (work: Work) =>
      Try {
        runner.run(workload.dnfSize, work.selectedDimensions, work.selectedRows).map { result =>
          dao.insertResult(schema = workload.name, result)
        }
      } match {
        case Failure(e) =>
          log.error(s"Failed to compute work:$work", e)
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

//  private def takeSample(data: Array[XYZ], sampleSize: Int, seed: Int): Array[XYZ] = {
//    val r = new Random(seed)
//    r.shuffle(data.toList).take(sampleSize).toArray
//  }
}
