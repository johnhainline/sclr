package cluster.sclr

import akka.Done
import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._
import combinations.{CombinationAggregation, CombinationBuilder}

class GiveWorkActor extends Actor with ActorLogging {
  private val combinations = new CombinationAggregation(Vector(new CombinationBuilder(4,3), new CombinationBuilder(2,2)))
  private val iterator = combinations.all()

  private val mediator = DistributedPubSub(context.system).mediator

  mediator ! DistributedPubSubMediator.Subscribe(topicRequestWork, Some(requestWorkGroup), self)

  override def receive: Receive = {
    case RequestWork(to) => {
      log.debug(s"GiveWorkActor <- RequestWork($to)")
      if (iterator.hasNext) {
        val next = iterator.next()
        log.debug(s"GiveWorkActor -> $next to $to")
        to ! next
      } else {
        log.debug(s"GiveWorkActor -> done to $to")
        to ! Done
      }
    }
  }
}

object GiveWorkActor {
  def props() = Props(new GiveWorkActor())
}
