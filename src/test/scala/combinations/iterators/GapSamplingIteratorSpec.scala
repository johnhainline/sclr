package combinations.iterators

import combinations.Combinations
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class GapSamplingIteratorSpec extends FlatSpec with Matchers {

  val r = new Random(123456)

  "GapSamplingIterator" should "handle a typical use case" in {
    val p = 0.4
    val combinations = Combinations(6,3).range(10, 45).toVector
    val result = GapSamplingIterator(Combinations(6,3).range(10, 45), p, r).toVector
    result should not equal combinations
    for (r <- result) {
      combinations.contains(r) shouldBe true
    }
    val expectedSize = Math.round(combinations.size * p).toInt
    result.size shouldBe expectedSize
  }

  it should "handle sampling from a large iterator" in {
    val p = 0.0000001
    val size = 1000000000
    val result = GapSamplingIterator(Range(0, size).iterator, p, r).toVector
    val expectedSize = Math.round(size * p).toInt
    result.size should (be > expectedSize / 2 and be < expectedSize * 2)
  }
}
