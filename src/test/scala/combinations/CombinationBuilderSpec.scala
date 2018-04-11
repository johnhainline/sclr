package combinations

import org.scalatest._

class CombinationBuilderSpec extends FlatSpec with Matchers {
  "Combinations" should "hold the total count of combos" in {
    CombinationBuilder.choose(10, 2) shouldEqual 45L
    CombinationBuilder.choose(10, 2) shouldEqual CombinationBuilder.choose(10, 8)
    CombinationBuilder.choose(5, 3) shouldEqual 10L
    CombinationBuilder.choose(6, 2) shouldEqual 15L
    CombinationBuilder.choose(10000, 5) shouldEqual 832500291625002000L
    CombinationBuilder.choose(10000, 5) shouldEqual CombinationBuilder.choose(10000, 9995)
    CombinationBuilder.choose(10000, 6) shouldEqual BigDecimal("1386806735798649165000")
  }

  it should "know the first and last combination of an n choose k" in {
    CombinationBuilder.first(1,1) shouldEqual Vector(0)
    CombinationBuilder.last(1,1)  shouldEqual Vector(0)

    CombinationBuilder.first(2,1) shouldEqual Vector(0)
    CombinationBuilder.last(2,1)  shouldEqual Vector(1)

    CombinationBuilder.first(10,1) shouldEqual Vector(0)
    CombinationBuilder.last(10,1)  shouldEqual Vector(9)

    CombinationBuilder.first(5,2) shouldEqual Vector(0,1)
    CombinationBuilder.last(5,2)  shouldEqual Vector(3,4)

    CombinationBuilder.first(6,3) shouldEqual Vector(0,1,2)
    CombinationBuilder.last(6,3)  shouldEqual Vector(3,4,5)

  }

  // For 6 choose 3 -> {0,1,3,3,4,5}
  // 013 < 013 < 033 < 133 < 014 < 034 < 134 < 034 < 134 < 334 < 015 < 035 < 135 < 035 < 135 < 335 < 045 < 145 < 345 < 345
  it should "use correct rank" in {
    CombinationBuilder.rank(Vector(0)) shouldEqual 0
    CombinationBuilder.rank(Vector(1)) shouldEqual 1

    CombinationBuilder.rank(Vector(0,1,2)) shouldEqual 0  // 012
    CombinationBuilder.rank(Vector(0,1,3)) shouldEqual 1  // 013
    CombinationBuilder.rank(Vector(0,2,3)) shouldEqual 2  // 023
    CombinationBuilder.rank(Vector(1,2,3)) shouldEqual 3  // 123
    CombinationBuilder.rank(Vector(0,1,4)) shouldEqual 4  // 014
    CombinationBuilder.rank(Vector(0,2,4)) shouldEqual 5  // 024
    CombinationBuilder.rank(Vector(1,2,4)) shouldEqual 6  // 124
    CombinationBuilder.rank(Vector(0,3,4)) shouldEqual 7  // 034
    CombinationBuilder.rank(Vector(1,3,4)) shouldEqual 8  // 134
    CombinationBuilder.rank(Vector(2,3,4)) shouldEqual 9  // 234
    CombinationBuilder.rank(Vector(0,1,5)) shouldEqual 10 // 015
    CombinationBuilder.rank(Vector(0,2,5)) shouldEqual 11 // 025
    CombinationBuilder.rank(Vector(1,2,5)) shouldEqual 12 // 125
    CombinationBuilder.rank(Vector(0,3,5)) shouldEqual 13 // 035
    CombinationBuilder.rank(Vector(1,3,5)) shouldEqual 14 // 135
    CombinationBuilder.rank(Vector(2,3,5)) shouldEqual 15 // 235
    CombinationBuilder.rank(Vector(0,4,5)) shouldEqual 16 // 045
    CombinationBuilder.rank(Vector(1,4,5)) shouldEqual 17 // 145
    CombinationBuilder.rank(Vector(2,4,5)) shouldEqual 18 // 245
    CombinationBuilder.rank(Vector(3,4,5)) shouldEqual 19 // 345
  }

  it should "use correct unrank" in {
    CombinationBuilder.unrank(1, 0) shouldEqual Vector(0)
    CombinationBuilder.unrank(1, 1) shouldEqual Vector(1)

    CombinationBuilder.unrank(3, 0)  shouldEqual Vector(0,1,2) // 012
    CombinationBuilder.unrank(3, 1)  shouldEqual Vector(0,1,3) // 013
    CombinationBuilder.unrank(3, 2)  shouldEqual Vector(0,2,3) // 023
    CombinationBuilder.unrank(3, 3)  shouldEqual Vector(1,2,3) // 123
    CombinationBuilder.unrank(3, 4)  shouldEqual Vector(0,1,4) // 014
    CombinationBuilder.unrank(3, 5)  shouldEqual Vector(0,2,4) // 024
    CombinationBuilder.unrank(3, 6)  shouldEqual Vector(1,2,4) // 124
    CombinationBuilder.unrank(3, 7)  shouldEqual Vector(0,3,4) // 034
    CombinationBuilder.unrank(3, 8)  shouldEqual Vector(1,3,4) // 134
    CombinationBuilder.unrank(3, 9)  shouldEqual Vector(2,3,4) // 234
    CombinationBuilder.unrank(3, 10) shouldEqual Vector(0,1,5) // 015
    CombinationBuilder.unrank(3, 11) shouldEqual Vector(0,2,5) // 025
    CombinationBuilder.unrank(3, 12) shouldEqual Vector(1,2,5) // 125
    CombinationBuilder.unrank(3, 13) shouldEqual Vector(0,3,5) // 035
    CombinationBuilder.unrank(3, 14) shouldEqual Vector(1,3,5) // 135
    CombinationBuilder.unrank(3, 15) shouldEqual Vector(2,3,5) // 235
    CombinationBuilder.unrank(3, 16) shouldEqual Vector(0,4,5) // 045
    CombinationBuilder.unrank(3, 17) shouldEqual Vector(1,4,5) // 145
    CombinationBuilder.unrank(3, 18) shouldEqual Vector(2,4,5) // 245
    CombinationBuilder.unrank(3, 19) shouldEqual Vector(3,4,5) // 345
  }

  it should "provide the correct next combination" in {
    CombinationBuilder.next(Vector(0)) shouldEqual Vector(1)
    CombinationBuilder.next(Vector(1)) shouldEqual Vector(2)

    CombinationBuilder.next(Vector(0,10)) shouldEqual Vector(1,10)
    CombinationBuilder.next(Vector(9,10)) shouldEqual Vector(0,11)

    CombinationBuilder.next(Vector(0,1,2)) shouldEqual Vector(0,1,3)
    CombinationBuilder.next(Vector(0,1,3)) shouldEqual Vector(0,2,3)
    CombinationBuilder.next(Vector(0,2,3)) shouldEqual Vector(1,2,3)
    CombinationBuilder.next(Vector(1,2,3)) shouldEqual Vector(0,1,4)
    CombinationBuilder.next(Vector(0,1,4)) shouldEqual Vector(0,2,4)
    CombinationBuilder.next(Vector(0,2,4)) shouldEqual Vector(1,2,4)
    CombinationBuilder.next(Vector(1,2,4)) shouldEqual Vector(0,3,4)
    CombinationBuilder.next(Vector(0,3,4)) shouldEqual Vector(1,3,4)
    CombinationBuilder.next(Vector(1,3,4)) shouldEqual Vector(2,3,4)
    CombinationBuilder.next(Vector(2,3,4)) shouldEqual Vector(0,1,5)
    CombinationBuilder.next(Vector(0,1,5)) shouldEqual Vector(0,2,5)
    CombinationBuilder.next(Vector(0,2,5)) shouldEqual Vector(1,2,5)
    CombinationBuilder.next(Vector(1,2,5)) shouldEqual Vector(0,3,5)
    CombinationBuilder.next(Vector(0,3,5)) shouldEqual Vector(1,3,5)
    CombinationBuilder.next(Vector(1,3,5)) shouldEqual Vector(2,3,5)
    CombinationBuilder.next(Vector(2,3,5)) shouldEqual Vector(0,4,5)
    CombinationBuilder.next(Vector(0,4,5)) shouldEqual Vector(1,4,5)
    CombinationBuilder.next(Vector(1,4,5)) shouldEqual Vector(2,4,5)
    CombinationBuilder.next(Vector(2,4,5)) shouldEqual Vector(3,4,5)
    CombinationBuilder.next(Vector(3,4,5)) shouldEqual Vector(0,1,6)
  }

  it should "provide an iterator for the combination" in {

    val result = CombinationBuilder(6,3).all().toVector

    result shouldEqual Vector(
      Vector(0,1,2), Vector(0,1,3), Vector(0,2,3), Vector(1,2,3), Vector(0,1,4),
      Vector(0,2,4), Vector(1,2,4), Vector(0,3,4), Vector(1,3,4), Vector(2,3,4),
      Vector(0,1,5), Vector(0,2,5), Vector(1,2,5), Vector(0,3,5), Vector(1,3,5),
      Vector(2,3,5), Vector(0,4,5), Vector(1,4,5), Vector(2,4,5), Vector(3,4,5)
    )
  }

  it should "provide an iterator for a section of the combination" in {
    CombinationBuilder(6, 3).rangeUntil(3, 8).toVector shouldEqual Vector(
      Vector(1,2,3), Vector(0,1,4), Vector(0,2,4), Vector(1,2,4), Vector(0,3,4)
    )
    CombinationBuilder(6, 3).rangeUntil(17, 20).toVector shouldEqual Vector(
      Vector(1,4,5), Vector(2,4,5), Vector(3,4,5)
    )
    CombinationBuilder(1, 1).rangeUntil(0, 1).toVector shouldEqual Vector(Vector(0))
    CombinationBuilder(2, 1).rangeUntil(0, 2).toVector shouldEqual Vector(Vector(0), Vector(1))
    CombinationBuilder(2, 1).rangeUntil(1, 2).toVector shouldEqual Vector(Vector(1))
    CombinationBuilder(9, 4).rangeUntil(116, 126).toVector shouldEqual Vector(
      Vector(1,5,7,8), Vector(2,5,7,8), Vector(3,5,7,8), Vector(4,5,7,8), Vector(0,6,7,8),
      Vector(1,6,7,8), Vector(2,6,7,8), Vector(3,6,7,8), Vector(4,6,7,8), Vector(5,6,7,8)
    )
  }

  it should "loop the iterator on a range that exceeds the combination" in {

    val result1 = CombinationBuilder(6,3).rangeUntil(10, 45).toVector
    result1 shouldEqual Vector(
      Vector(0,1,5), Vector(0,2,5), Vector(1,2,5), Vector(0,3,5), Vector(1,3,5),
      Vector(2,3,5), Vector(0,4,5), Vector(1,4,5), Vector(2,4,5), Vector(3,4,5),

      Vector(0,1,2), Vector(0,1,3), Vector(0,2,3), Vector(1,2,3), Vector(0,1,4),
      Vector(0,2,4), Vector(1,2,4), Vector(0,3,4), Vector(1,3,4), Vector(2,3,4),
      Vector(0,1,5), Vector(0,2,5), Vector(1,2,5), Vector(0,3,5), Vector(1,3,5),
      Vector(2,3,5), Vector(0,4,5), Vector(1,4,5), Vector(2,4,5), Vector(3,4,5),

      Vector(0,1,2), Vector(0,1,3), Vector(0,2,3), Vector(1,2,3), Vector(0,1,4)
    )

    val result2 = CombinationBuilder(5,2).rangeUntil(21, 32).toVector
    result2 shouldEqual Vector(
      Vector(0,2), Vector(1,2), Vector(0,3), Vector(1,3), Vector(2,3), Vector(0,4), Vector(1,4), Vector(2,4), Vector(3,4), Vector(0,1),
      Vector(0,2)
    )
  }

  it should "provide a random iterator that hits every combination" in {
    val p = 0.4
    val result1 = GapSamplingIterator(CombinationBuilder(6,3).rangeUntil(10, 45), p).toVector
    val all = Vector(
      Vector(0,1,5), Vector(0,2,5), Vector(1,2,5), Vector(0,3,5), Vector(1,3,5),
      Vector(2,3,5), Vector(0,4,5), Vector(1,4,5), Vector(2,4,5), Vector(3,4,5),

      Vector(0,1,2), Vector(0,1,3), Vector(0,2,3), Vector(1,2,3), Vector(0,1,4),
      Vector(0,2,4), Vector(1,2,4), Vector(0,3,4), Vector(1,3,4), Vector(2,3,4),
      Vector(0,1,5), Vector(0,2,5), Vector(1,2,5), Vector(0,3,5), Vector(1,3,5),
      Vector(2,3,5), Vector(0,4,5), Vector(1,4,5), Vector(2,4,5), Vector(3,4,5),

      Vector(0,1,2), Vector(0,1,3), Vector(0,2,3), Vector(1,2,3), Vector(0,1,4)
    )

    result1 should not equal all

    val sampleSize = all.map { v =>
      if (result1.contains(v)) 1 else 0
    }.sum
    sampleSize shouldBe Math.round(all.size * p).toInt
  }
}
