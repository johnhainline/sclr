package cluster.main

import akka.actor.Actor
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages
import cluster.sclr.Messages.Finished

class Terminator extends Actor {
  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(Messages.topicStatus, self)
  def receive: Receive = {
    case Finished =>
      context.system.terminate()
  }
}
