package cluster.sclr

import scala.util.Try

object Messages {
  final case class Work(lookups: Vector[combinations.Combination])
  final case class Data(something: String)
  final case class Result(attempt: Try[Data])

  final case object Ready
  final case object GetWork
  final case object Finished

  val topicComputer = "worker"
  val topicManager  = "manager"
  val topicStatus   = "status"
}
