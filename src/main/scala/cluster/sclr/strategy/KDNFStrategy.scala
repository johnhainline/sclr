package cluster.sclr.strategy

import cluster.sclr.Messages.Work
import cluster.sclr.database.{Dataset, Result}

abstract class KDNFStrategy(dataset: Dataset) {
  def run(work: Work): Result
}
