package cluster.sclr

import combinations.CombinationAggregation

object Messages {
  // Send `Begin` to the ManageActor to start work.
  final case class Begin(combinationAggregation: CombinationAggregation)
  final case object BeginAck // acknowledgement response

  // ManageActor sends `Work` or `Finished` as a response to actors who send it the GetWork message.
  final case class Work(lookups: Vector[combinations.Combination])
  final case object Finished

  // DistributedPubSub messages.
  // Ready is sent by ManageActor to topicComputer and topicStatus.
  final case object Ready
  // GetWork is sent by ComputeActor to topicManager.
  final case object GetWork

  // DistributedPubSub Topics.
  val topicComputer = "worker"
  val topicManager  = "manager"
  val topicStatus   = "status"
}
