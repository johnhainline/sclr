package cluster.sclr

import akka.updated.stream.{SinkRef, SourceRef}
import cluster.sclr.core.Result

object Messages {
  val workloadTopic = "workloadTopic"

  final case class Workload(name: String, dnfSize: Int, mu: Double, useLPNorm: Boolean, optionalEpsilon: Option[Double] = None, optionalSubset: Option[Int] = None) {
    def getRowsConstant() = {
      if (useLPNorm) 2 else 3
    }
  }

  final case class Work(selectedDimensions: Vector[Int], selectedRows: Vector[Int])
  final case class WorkComputeReady(pullWork: SinkRef[Work], pushResult: SourceRef[Result])
}
