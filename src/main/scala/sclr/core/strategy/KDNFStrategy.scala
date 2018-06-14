package sclr.core.strategy

import sclr.core.Messages.Work
import sclr.core.database.{Dataset, Result}

abstract class KDNFStrategy(dataset: Dataset) {
  def run(work: Work): Result
}
