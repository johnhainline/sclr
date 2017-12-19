package cluster.sclr

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import cluster.sclr.Messages._
import cluster.sclr.SaveResultActor.AskForResult

class SaveResultActor extends Actor with ActorLogging {

  val mediator = DistributedPubSub(context.system).mediator

  private def askForResult() = {
    mediator ! Publish(topicRequestResult, RequestResult(self), sendOneMessageToEachGroup = true)
  }

  def receive = {
    case AskForResult => {
      log.debug(s"SaveResultActor -> RequestResult($self)")
      askForResult()
    }
    case result: Result => {
      log.debug(s"SaveResultActor <- $result")
      // Save result!
      log.debug(s"SaveResultActor -> RequestResult($self)")
      askForResult()
    }
  }
}

object SaveResultActor {
  case object AskForResult
  def props() = Props(new SaveResultActor())
}
