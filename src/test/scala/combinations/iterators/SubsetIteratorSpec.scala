package combinations.iterators

import combinations.Combinations
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class SubsetIteratorSpec extends FlatSpec with Matchers {

  val r = new Random(123456)

  "SubsetCombinationsIterator" should "give back the correct number of combinations" in {
    val sample = 10
    val combinations = Combinations(1000, 3)
    val result = combinations.subsetIterator(sample, r).toVector
    result.size shouldBe Combinations(10, 3).size.toInt
  }

  it should "only return a specific generated subset of entries" in {
    val sampleSize = 100
    val (n, k) = (1000000, 2)
    val c = Combinations(n, k)
    val result = c.subsetIterator(sampleSize, r).toVector

    result.size shouldBe Combinations(sampleSize, k).size.toInt
    result.flatten.distinct.size shouldBe sampleSize

    var max = 0
    for (sample <- result) {
      sample.max should be >= max
      max = sample.max
      for (s <- sample) {
        s >= 0 shouldBe true
        s <  n shouldBe true
      }
    }
    max should be > sampleSize
  }
}
