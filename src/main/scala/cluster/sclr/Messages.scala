package cluster.sclr

import akka.actor.ActorRef

object Messages {
  final case class RequestWork(actor: ActorRef)
  final case class RequestResult(actor: ActorRef)

  final case class Job(lookups: Vector[combinations.Combination])
  final case class JobComplete(text: String)
  final case class JobFailed(error: String)

  val topicRequestWork = "requestWork"
  val topicRequestResult = "requestResult"

  val requestWorkGroup = "workGroup"
  val requestResultGroup = "resultGroup"

  final case object ProcessingComplete
  val topicProcessingComplete = "processingComplete"
}
