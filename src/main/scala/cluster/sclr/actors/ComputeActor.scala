package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._
import cluster.sclr.doobie.ResultsDao
import doobie.implicits._

import scala.util.{Success, Try}

class ComputeActor extends Actor with ActorLogging {

  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topicComputer, self)

  private def askForWork(): Unit = {
    mediator ! Publish(topicManager, GetWork)
  }

  private lazy val xa = ResultsDao.makeSimpleTransactor()

  private def setupDatabase() = {
    try {
      ResultsDao.createSchemaIfNeeded()
      ResultsDao.up1.run.transact(xa).unsafeRunSync()
    } catch {
      case e: Exception =>
        log.error(e, "Could not set up database.")
        throw e
    }
  }

  override def preStart(): Unit = {
    setupDatabase()
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
        Data(work.lookups.toString())
      } match {
        case Success(data) =>
          try {
            ResultsDao.insertResult(data.something).run.transact(xa).unsafeRunSync()
          } catch {
            case e: Exception => e.printStackTrace()
          }
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
  def props() = Props(new ComputeActor())
}
