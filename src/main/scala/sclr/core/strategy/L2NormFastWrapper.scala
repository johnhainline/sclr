package sclr.core.strategy

import ch.jodersky.jni.nativeLoader
import sclr.core.Messages.Work
import sclr.core.database.{Dataset, Result}

@nativeLoader("l2normfast0")
class L2NormFastWrapper {
  @native def prepare(dataset: Dataset, name: String, dnfSize: Int, mu: Double, epsilon: Double, subset: Int, randomSeed: Int): Int
  @native def run(work: Work): Result
}
