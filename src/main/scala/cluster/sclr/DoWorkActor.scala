package cluster.sclr

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import cluster.sclr.Messages._

import scala.collection.mutable

class DoWorkActor extends Actor with ActorLogging {
  val requests = new mutable.Queue[ActorRef]()

  val mediator = DistributedPubSub(context.system).mediator

  mediator ! DistributedPubSubMediator.Subscribe(topicRequestResult, Some(requestResultGroup), self)

  private def askForWork() = {
    mediator ! Publish(topicRequestWork, RequestWork(self), sendOneMessageToEachGroup = true)
  }

  def receive = {
    case RequestResult(to) => {
      log.debug(s"DoWorkActor <- RequestResult($to)")
      requests.enqueue(to)
      askForWork()
    }
    case work: Work => {
      val result = work
      val to = requests.dequeue()
      log.debug(s"DoWorkActor -> $work to $to")
      to ! result
    }
  }
}

object DoWorkActor {
  def props() = Props(new DoWorkActor())
}
