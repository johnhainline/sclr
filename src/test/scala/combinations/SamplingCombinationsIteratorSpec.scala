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
    val sampleSize = 1000
    val (n, k) = (1000000, 20)
    val c = Combinations(n, k)

    val result = c.samplingIterator(sampleSize, r).toVector
    val max = c.size

    result.size shouldBe 1000
    result.distinct.size shouldBe 1000
    for (sample <- result) {
      for (s <- sample) {
        s >= 0   shouldBe true
        s <= max shouldBe true
      }
    }
  }
}
