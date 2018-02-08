package cluster.sclr

import combinations.Combination

object Messages {

  // Send Workload to the ManageActor to start work. ManageActor sends `Workload` to topicStatus.
  final case class Workload(name: String, selectXDimensions: Int, selectXRows: Int)
  final case object Ack // acknowledgement response

  // GetWork is sent by ComputeActor to topicManager (i.e. ManageActor).
  final case object GetWork

  // ManageActor sends WorkConfig to topicComputer (sent every 5 sec, picked up by ComputeActors).
  final case class WorkConfig(name: String, randomSeed: Int, sampleSize: Int)
  // ManageActor sends `Work` or `Finished` as a response to actors who send it the GetWork message.
  final case class Work(selectedDimensions: Vector[Int], selectedRows: Vector[Int])
  final case object Finished
  // ManageActor responds to Status message with how far along the current Workload is (if running a Workload).
  final case object Status
  final case class StatusResponse(workload: Workload, iterator: Option[Vector[Combination]])

  // DistributedPubSub Topics.
  val topicComputer = "worker"
  val topicManager  = "manager"
  val topicStatus   = "status"
}
