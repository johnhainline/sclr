package cluster.sclr

import akka.actor.ActorRef


object Messages {
  final case class RequestWork(actor: ActorRef)
  final case class RequestResult(actor: ActorRef)

  type Work = Vector[combinations.Combination]
  type Result = Vector[combinations.Combination]

  val topicRequestWork = "requestWork"
  val topicRequestResult = "requestResult"

  val requestWorkGroup = "workGroup"
  val requestResultGroup = "resultGroup"
}
