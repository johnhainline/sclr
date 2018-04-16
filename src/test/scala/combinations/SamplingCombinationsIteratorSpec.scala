package combinations

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class SamplingCombinationsIteratorSpec extends FlatSpec with Matchers {

  val r = new Random(123456)

  "SamplingCombinationsIterator" should "handle a typical use case" in {
    val sample = 10
    val combinations = Combinations(1000, 3)
    val result = combinations.samplingIterator(sample, r).toVector
    result.size shouldBe sample
  }

  it should "handle sampling from a massive iterator" in {
    val sample = 1000

    val result = Combinations(1000000, 20).samplingIterator(sample, r).toVector

    result.size should (be > 999 and be < 1001)
  }
}
