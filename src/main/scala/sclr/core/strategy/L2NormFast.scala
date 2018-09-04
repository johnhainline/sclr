package sclr.core.strategy

import sclr.core.Messages.{Work, Workload}
import sclr.core.database.{Dataset, Result}

class L2NormFast(val dataset: Dataset, val workload: Workload) extends KDNFStrategy {
  val wrapper = new L2NormFastWrapper()
  wrapper.prepare(dataset, workload)
  override def run(work: Work): Result = {
    wrapper.run(work)
  }
}
