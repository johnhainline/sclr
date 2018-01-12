package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._
import cluster.sclr.actors.ManageActor.SendReadyMessage

import scala.concurrent.duration._

class ManageActor extends Actor with ActorLogging {

  private var iterator: Iterator[Vector[combinations.Combination]] = _
  private var sendSchedule: Cancellable = _

  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topicManager, self)

  def waiting: Receive = {
    case Begin(combinationAggregation) =>
      iterator = combinationAggregation.all()
      log.debug("waiting -> sending")
      context.become(sending)
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.language.postfixOps
      sendSchedule = context.system.scheduler.schedule(0 seconds, 5 seconds, self, SendReadyMessage)
      mediator ! Publish(topicStatus, Ready)
  }

  def sending: Receive = {
    case SendReadyMessage =>
      mediator ! Publish(topicComputer, Ready)
      log.debug("sending Ready")
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
  private case object SendReadyMessage
  def props() = Props(new ManageActor())
}
