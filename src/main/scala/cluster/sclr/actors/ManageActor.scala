package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._

import scala.concurrent.duration._

class ManageActor extends Actor with ActorLogging {

  private var iterator: Iterator[Vector[combinations.Combination]] = _
  private var sendSchedule: Cancellable = _

  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topicManager, self)

  def waiting: Receive = {
    case Begin(combinationAggregation, csvFilename) =>
      iterator = combinationAggregation.all()
      log.debug("waiting -> sending")
      context.become(sending)
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.language.postfixOps
      val readyMsg = Ready(csvFilename)
      sendSchedule = context.system.scheduler.schedule(0 seconds, 5 seconds, self, readyMsg)
      mediator ! Publish(topicStatus, readyMsg)
      sender() ! BeginAck
  }

  def sending: Receive = {
    case readyMsg:Ready =>
      mediator ! Publish(topicComputer, readyMsg)
      log.debug(s"sending Ready(${readyMsg.csvFilename})")
    case GetWork =>
      if (iterator.hasNext) {
        val next = iterator.next()
        sender() ! Work(next)
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
  def props() = Props(new ManageActor())
}
