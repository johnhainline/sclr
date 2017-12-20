package cluster.sclr

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._

import scala.collection.mutable

class DoWorkActor extends Actor with ActorLogging {
  var done = false
  val requests = new mutable.Queue[ActorRef]()

  val mediator = DistributedPubSub(context.system).mediator

  mediator ! DistributedPubSubMediator.Subscribe(topicRequestResult, Some(requestResultGroup), self)

  private def askForWork() = {
    mediator ! Publish(topicRequestWork, RequestWork(self), sendOneMessageToEachGroup = true)
  }

  def receive = {
    case RequestResult(to) => {
//      log.debug(s"DoWorkActor <- RequestResult($to)")
      if (done) {
        to ! Done
      } else {
        requests.enqueue(to)
        askForWork()
      }
    }
    case Job(lookups) => {
      val result = lookups.toString()
      val to = requests.dequeue()
//      log.debug(s"DoWorkActor -> $work to $to")
      to ! JobComplete(result)
    }
    case Done => {
      val to = requests.dequeue()
      to ! Done
      done = true
    }
  }
}

object DoWorkActor {
  def props() = Props(new DoWorkActor())
}
