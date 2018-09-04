package sclr.core.strategy

import ch.jodersky.jni.nativeLoader
import sclr.core.Messages.{Work, Workload}
import sclr.core.database.{Dataset, Result}

@nativeLoader("l2normfast1")
class L2NormFastWrapper {
  @native def prepare(dataset: Dataset, workload: Workload): Int
  @native def run(work: Work): Result
}
