package cluster.sclr

object Messages {
  final case class Work(lookups: Vector[combinations.Combination])

  final case object Ready
  final case object GetWork
  final case object Finished

  val topicComputer = "worker"
  val topicManager  = "manager"
  val topicStatus   = "status"
}
