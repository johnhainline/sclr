package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._
import cluster.sclr.doobie.ResultsDao
import weka.Regression

import scala.util.{Failure, Try}

class ComputeActor(resultsDao: ResultsDao) extends Actor with ActorLogging {

  private var regression: Regression = _
  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topicComputer, self)

  private def askForWork(): Unit = {
    mediator ! Publish(topicManager, GetWork)
  }

  def waiting: Receive = {
    case Ready(csvFilename) =>
      regression = new Regression(csvFilename)
      log.debug("waiting -> computing")
      context.become(computing)
      askForWork()
  }

  def computing: Receive = {
    case (work: Work) =>
      Try {
        val (dim1, dim2) = (work.lookups.head(0), work.lookups.head(1))
        val (row1, row2, row3) = (work.lookups.last(0), work.lookups.last(1), work.lookups.last(2))
        val coefficients = regression.linearRegressionSubAttributes(work.lookups.head, work.lookups.last)
        resultsDao.insertResult(isGood = true, dim1, dim2, row1, row2, row3, coefficients(0), coefficients(1), coefficients(2))
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
