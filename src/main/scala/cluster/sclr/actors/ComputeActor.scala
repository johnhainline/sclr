package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._
import cluster.sclr.doobie.ResultsDao

import scala.util.{Failure, Try}

class ComputeActor(resultsDao: ResultsDao) extends Actor with ActorLogging {

  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topicComputer, self)

  private def askForWork(): Unit = {
    mediator ! Publish(topicManager, GetWork)
  }

  def waiting: Receive = {
    case Ready =>
      log.debug("waiting -> computing")
      context.become(computing)
      askForWork()
  }

  def computing: Receive = {
    case (work: Work) =>
      Try {
        val data = work.lookups.toString()
        resultsDao.insertResult(data)
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
  def props(resultsDao: ResultsDao) = Props(new ComputeActor(resultsDao))
}
