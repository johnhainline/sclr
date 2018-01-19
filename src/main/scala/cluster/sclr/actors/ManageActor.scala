package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._
import cluster.sclr.doobie.ResultsDao
import combinations.{CombinationAggregation, CombinationBuilder}

import scala.concurrent.duration._

class ManageActor(resultsDao: ResultsDao) extends Actor with ActorLogging {

  private var workload: Workload = _
  private var iterator: Iterator[Vector[combinations.Combination]] = _
  private var sendSchedule: Cancellable = _

  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topicManager, self)

  def waiting: Receive = {
    case work: Workload =>
      resultsDao.setupDatabase()
      resultsDao.setupTable(work.name, work.selectDimensions, work.selectRows, work.selectDimensions + 1)

      iterator = CombinationAggregation(Vector(
          CombinationBuilder(work.totalDimensions, work.selectDimensions),
          CombinationBuilder(work.totalRows, work.selectRows))).all()
      workload = work
      log.debug("waiting -> sending")
      context.become(sending)
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.language.postfixOps
      sendSchedule = context.system.scheduler.schedule(0 seconds, 5 seconds, self, workload)
      mediator ! Publish(topicStatus, workload)
      sender() ! Ack
  }

  def sending: Receive = {
    case workload:Workload =>
      mediator ! Publish(topicComputer, workload)
      log.debug(s"sending $workload")
    case GetWork =>
      if (iterator.hasNext) {
        val next = iterator.next()
        sender() ! Work(next.head, next.last)
        log.debug("sending Work")
      } else {
        log.debug("sending -> finished")
        context.become(finished)
        sendSchedule.cancel()
        sender() ! Finished
        mediator ! Publish(topicStatus, Finished)
      }
  }

  def finished: Receive = {
    case GetWork =>
      sender() ! Finished
  }

  def receive: Receive = waiting
}

object ManageActor {
  def props(resultsDao: ResultsDao) = Props(new ManageActor(resultsDao))
}
