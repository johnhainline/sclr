package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._
import cluster.sclr.core.{DatabaseDao, WorkloadRunner}
import weka.core.Instances
import weka.filters.Filter
import weka.filters.unsupervised.instance.Resample

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
    case workConfig:WorkConfig =>
      name = workConfig.name
      val dataset = dao.getDataset(name)
      val sampleX = sampleInstances(dataset.x, workConfig.sampleSize, workConfig.randomSeed)
      runner = new WorkloadRunner(sampleX, dataset.yz)
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

  private def sampleInstances(x: Instances, sampleSize: Int, randomSeed: Int): Instances = {
    val filter = new Resample()
    val sampleSizePercent = sampleSize.toDouble / x.size().toDouble
    filter.setInputFormat(x)
    filter.setSampleSizePercent(sampleSizePercent)
    filter.setNoReplacement(true)
    filter.setRandomSeed(randomSeed)
    Filter.useFilter(x, filter)
  }
}

object ComputeActor {
  def props(resultsDao: DatabaseDao) = Props(new ComputeActor(resultsDao))
}
