package sclr.core

import akka.stream.{SinkRef, SourceRef}
import sclr.core.database.Result

object Messages {
  val workloadTopic = "workloadTopic"

  final case class Workload(name: String, dnfSize: Int, mu: Double, useLPNorm: Boolean,
                            optionalEpsilon: Option[Double] = None,
                            optionalSubset: Option[Int] = None,
                            optionalRandomSeed: Option[Int] = None) {
    def getRowsConstant() = {
      if (useLPNorm) 2 else 3
    }
  }

  // This is a version of the workload sent by the ManageActor to its ComputeActors. We need to differentiate between
  // different workload runs so we add an id.
  final case class ActiveWorkload(id: Int, workload: Workload)

  final case class Work(index: Int, selectedDimensions: Vector[Int], selectedRows: Vector[Int])
  final case class WorkComputeReady(pullWork: SinkRef[Work], pushResult: SourceRef[Result], computeCountOption: Option[Int])
}
