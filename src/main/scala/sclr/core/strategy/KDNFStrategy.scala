package sclr.core.strategy

import sclr.core.Messages.Work
import sclr.core.database.Result

trait KDNFStrategy {
  def run(work: Work): Result
}
