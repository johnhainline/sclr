package cluster.sclr

import akka.Done
import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import cluster.sclr.Messages._
import cluster.sclr.SaveResultActor.AskForResult

class SaveResultActor() extends Actor with ActorLogging {
  var activeRequests = 0
  var done = false

  val mediator = DistributedPubSub(context.system).mediator

  private def askForResult() = {
    if (!done) {
      activeRequests += 1
      mediator ! Publish(topicRequestResult, RequestResult(self), sendOneMessageToEachGroup = true)
      log.debug(s"activeRequests+1 : $activeRequests")
    }
  }

  def receive = {
    case AskForResult => {
//      log.debug(s"SaveResultActor -> RequestResult($self)")
      askForResult()
    }
    case JobComplete(result) => {
      activeRequests -= 1
      log.debug(s"activeRequests-1 : $activeRequests")
//      log.debug(s"SaveResultActor <- $result")
      // Save result!
      log.debug(s"$result")
//      log.debug(s"SaveResultActor -> RequestResult($self)")
      askForResult()
      if (activeRequests == 0 && done) {
        context.system.terminate()
      }
    }
    case Done => {
      activeRequests -= 1
      done = true
      log.debug(s"activeRequests-1 : $activeRequests")
      if (activeRequests == 0 && done) {
        context.system.terminate()
      }
    }
  }
}

object SaveResultActor {
  case object AskForResult
  def props() = Props(new SaveResultActor())
}
