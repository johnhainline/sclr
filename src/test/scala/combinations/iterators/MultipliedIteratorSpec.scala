package combinations.iterators

import sclr.core.actors.ManageActor
import combinations.Combinations
import org.scalatest._

import scala.util.Random

class MultipliedIteratorSpec extends FlatSpec with Matchers {

  "CombinationsAggregation" should "hold the total count of combos" in {
    val test1 = MultipliedIterator(Vector(() => Combinations(10,2).iterator(), () => Combinations(3,1).iterator())).toVector
    test1.length shouldEqual 135

    val test2 = MultipliedIterator(Vector(() => Combinations(5,2).iterator(), () => Combinations(6,2).iterator())).toVector
    test2.length shouldEqual 150
  }

  // For 6 choose 3 -> {0,1,3,3,4,5}
  it should "use correct rank" in {
    Combinations.rank(Array(Array(0), Array(1))) shouldEqual Array((1,0), (1,1))
    Combinations.rank(Array(Array(0,1,2), Array(0,3,4,5))) shouldEqual Array((3,0), (4,12))
    Combinations.rank(Array(Array(2,3,5), Array(0,1,2), Array(8,0))) shouldEqual Array((3,15), (3,0), (2,8))
  }

  it should "use correct unrank" in {
    Combinations.unrank(Array((3, 7), (2, 0))) shouldEqual Array(Array(0,3,4), Array(0,1))
    Combinations.unrank(Array((3,16), (3,10))) shouldEqual Array(Array(0,4,5), Array(0,1,5))
  }

  it should "provide an iterator for the combination" in {

    val result = MultipliedIterator(Vector(() => Combinations(4,3).iterator(), () => Combinations(5,2).iterator(), () => Combinations(2,1).iterator())).toArray

    val combo1 = Combinations(4,3).iterator().toArray
    val combo2 = Combinations(5,2).iterator().toArray
    val combo3 = Combinations(2,1).iterator().toArray
    val expected = for (a <- combo1; b <- combo2; c <- combo3) yield {
      Array(a,b,c)
    }
    result shouldEqual expected
  }

  it should "provide an iterator for sampled combinations" in {
    val seed = 1234
    val combo1 = () => Combinations(4,3).iterator()
    val combo2 = () => Combinations(5,2).samplingIterator(5, new Random(seed))

    val result = MultipliedIterator(Vector(combo1, combo2)).toArray

    val expected = (for (a <- combo1(); b <- combo2()) yield {
      Array(a,b)
    }).toArray

    result shouldEqual expected
  }

  it should "provide an iterator for a section of combinations" in {

    val result1 = MultipliedIterator(Vector(() => Combinations(3,2).range(0,1), () => Combinations(2,1).range(0,1), () => Combinations(5,2).range(3,5))).toArray
    val expected1 = Array(
//      Array(Array(0,1),Array(0),Array(0,1)), // 0
//      Array(Array(0,1),Array(0),Array(0,2)), // 1
//      Array(Array(0,1),Array(0),Array(1,2)), // 2
      Array(Array(0,1),Array(0),Array(0,3)), // 3
      Array(Array(0,1),Array(0),Array(1,3))  // 4
    )
    result1 shouldEqual expected1

    val result2 = MultipliedIterator(Vector(() => Combinations(3,2).range(1,2), () => Combinations(2,1).range(0,1), () => Combinations(5,2).iterator())).toArray
    val expected2 = Array(
//      Array(Array(0,1),Array(1),Array(3,4)), // 19
      Array(Array(0,2),Array(0),Array(0,1)), // 20
      Array(Array(0,2),Array(0),Array(0,2)), // 21
      Array(Array(0,2),Array(0),Array(1,2)), // 22
      Array(Array(0,2),Array(0),Array(0,3)), // 23
      Array(Array(0,2),Array(0),Array(1,3)), // 24
      Array(Array(0,2),Array(0),Array(2,3)), // 25
      Array(Array(0,2),Array(0),Array(0,4)), // 26
      Array(Array(0,2),Array(0),Array(1,4)), // 27
      Array(Array(0,2),Array(0),Array(2,4)), // 28
      Array(Array(0,2),Array(0),Array(3,4))  // 29
//      Array(Array(0,2),Array(1),Array(0,1)), // 30
//      Array(Array(0,2),Array(1),Array(0,2))  // 31
    )
    result2 shouldEqual expected2

    val result3 = MultipliedIterator(Vector(() => Combinations(3,2).iterator(), () => Combinations(2,1).iterator())).toArray
    val expected3 = Array(
      Array(Array(0,1),Array(0)), // 0
      Array(Array(0,1),Array(1)), // 1
      Array(Array(0,2),Array(0)), // 2
      Array(Array(0,2),Array(1)), // 3
      Array(Array(1,2),Array(0)), // 4
      Array(Array(1,2),Array(1))  // 5
    )
    result3 shouldEqual expected3
  }

  it should "handle our particular usage correctly" in {
    val subsetSize = 20
    val rowCount = 5000
    val yLength = 10
    val expectedRows = Combinations(rowCount, 2).subsetIterator(subsetSize, new Random(new Random(1234).nextLong())).samples

    val iterator = ManageActor.createIterator(rowCount, yLength, rowsConstant = 2, Some(subsetSize), Some(1))

    val allWork = iterator.toArray
    val row0Entries = allWork.map(_.selectedRows.head)
    val row1Entries = allWork.map(_.selectedRows.last)
    val entries = row0Entries.toSet.union(row1Entries.toSet)
    entries should be (expectedRows.toSet)
    expectedRows.length should be (subsetSize)
    val expectedWorkSize = Combinations.choose(yLength, 2) * Combinations.choose(subsetSize, 2)
    allWork.size should be (expectedWorkSize)
  }
}
