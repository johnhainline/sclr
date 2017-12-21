package cluster.sclr

import akka.Done
import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import cluster.sclr.Messages._
import cluster.sclr.SaveResultActor.AskForResult

class SaveResultActor() extends Actor with ActorLogging {
  private var activeRequests = 0
  private var done = false

  private val mediator = DistributedPubSub(context.system).mediator

  private def askForResult() = {
    if (!done) {
      activeRequests += 1
      mediator ! Publish(topicRequestResult, RequestResult(self), sendOneMessageToEachGroup = true)
      log.debug(s"activeRequests+1 : $activeRequests")
    }
  }

  private def checkForCompletion() = {
    if (activeRequests == 0 && done) {
      mediator ! Publish(topicProcessingComplete, ProcessingComplete)
    }
  }

  def receive = {

    case AskForResult => {
      askForResult()
    }

    case JobComplete(result) => {
      activeRequests -= 1
      log.debug(s"activeRequests-1 : $activeRequests")
      // Save result!
      log.debug(s"$result")
      askForResult()
      checkForCompletion()
    }

    case Done => {
      activeRequests -= 1
      done = true
      log.debug(s"activeRequests-1 : $activeRequests")
      checkForCompletion()
    }
  }
}

object SaveResultActor {
  case object AskForResult
  def props() = Props(new SaveResultActor())
}
