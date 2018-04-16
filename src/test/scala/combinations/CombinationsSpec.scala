package combinations

import org.scalatest._

class CombinationsSpec extends FlatSpec with Matchers {
  "Combinations" should "hold the total count of combos" in {
    Combinations.choose(10, 2) shouldEqual 45L
    Combinations.choose(10, 2) shouldEqual Combinations.choose(10, 8)
    Combinations.choose(5, 3) shouldEqual 10L
    Combinations.choose(6, 2) shouldEqual 15L
    Combinations.choose(10000, 5) shouldEqual 832500291625002000L
    Combinations.choose(10000, 5) shouldEqual Combinations.choose(10000, 9995)
    Combinations.choose(10000, 6) shouldEqual BigDecimal("1386806735798649165000")
  }

  it should "know the first and last combination of an n choose k" in {
    Combinations.first(1,1) shouldEqual Vector(0)
    Combinations.last(1,1)  shouldEqual Vector(0)

    Combinations.first(2,1) shouldEqual Vector(0)
    Combinations.last(2,1)  shouldEqual Vector(1)

    Combinations.first(10,1) shouldEqual Vector(0)
    Combinations.last(10,1)  shouldEqual Vector(9)

    Combinations.first(5,2) shouldEqual Vector(0,1)
    Combinations.last(5,2)  shouldEqual Vector(3,4)

    Combinations.first(6,3) shouldEqual Vector(0,1,2)
    Combinations.last(6,3)  shouldEqual Vector(3,4,5)

  }

  it should "give correct min/max bounds for a combination" in {
    def checkBounds(actual: Int, k: Int, index: BigInt): Boolean = {
      val (low, high) = Combinations.boundsOfNGivenIndex(k, index)
      low <= actual && actual <= high
    }
    checkBounds(0,  1, index=1)    shouldBe true
    checkBounds(1,  1, index=2)    shouldBe true
    checkBounds(2,  1, index=3)    shouldBe true
    checkBounds(3,  1, index=4)    shouldBe true
    checkBounds(54, 1, index=55)   shouldBe true
    checkBounds(89, 2, index=4000) shouldBe true
    checkBounds(952327, 2,      index=BigInt(453463147950L)) shouldBe true
    checkBounds(711988, 2,      index=BigInt(253463147950L)) shouldBe true
    checkBounds(800001, 400000, index=BigInt(4000000))       shouldBe true
  }

  // For 6 choose 3 -> {0,1,3,3,4,5}
  // 013 < 013 < 033 < 133 < 014 < 034 < 134 < 034 < 134 < 334 < 015 < 035 < 135 < 035 < 135 < 335 < 045 < 145 < 345 < 345
  it should "use correct rank" in {
    Combinations.rank(Vector(0)) shouldEqual 0
    Combinations.rank(Vector(1)) shouldEqual 1
    Combinations.rank(Vector(2)) shouldEqual 2
    Combinations.rank(Vector(3)) shouldEqual 3
    Combinations.rank(Vector(4)) shouldEqual 4

    Combinations.rank(Vector(0,1,2)) shouldEqual 0  // 012
    Combinations.rank(Vector(0,1,3)) shouldEqual 1  // 013
    Combinations.rank(Vector(0,2,3)) shouldEqual 2  // 023
    Combinations.rank(Vector(1,2,3)) shouldEqual 3  // 123
    Combinations.rank(Vector(0,1,4)) shouldEqual 4  // 014
    Combinations.rank(Vector(0,2,4)) shouldEqual 5  // 024
    Combinations.rank(Vector(1,2,4)) shouldEqual 6  // 124
    Combinations.rank(Vector(0,3,4)) shouldEqual 7  // 034
    Combinations.rank(Vector(1,3,4)) shouldEqual 8  // 134
    Combinations.rank(Vector(2,3,4)) shouldEqual 9  // 234
    Combinations.rank(Vector(0,1,5)) shouldEqual 10 // 015
    Combinations.rank(Vector(0,2,5)) shouldEqual 11 // 025
    Combinations.rank(Vector(1,2,5)) shouldEqual 12 // 125
    Combinations.rank(Vector(0,3,5)) shouldEqual 13 // 035
    Combinations.rank(Vector(1,3,5)) shouldEqual 14 // 135
    Combinations.rank(Vector(2,3,5)) shouldEqual 15 // 235
    Combinations.rank(Vector(0,4,5)) shouldEqual 16 // 045
    Combinations.rank(Vector(1,4,5)) shouldEqual 17 // 145
    Combinations.rank(Vector(2,4,5)) shouldEqual 18 // 245
    Combinations.rank(Vector(3,4,5)) shouldEqual 19 // 345
  }

  it should "use correct unrank" in {
    Combinations.unrank(1, index=0) shouldEqual Vector(0)
    Combinations.unrank(1, index=1) shouldEqual Vector(1)
    Combinations.unrank(1, index=2) shouldEqual Vector(2)
    Combinations.unrank(1, index=3) shouldEqual Vector(3)
    Combinations.unrank(1, index=4) shouldEqual Vector(4)

    Combinations.unrank(3, index=0)  shouldEqual Vector(0,1,2) // 012
    Combinations.unrank(3, index=1)  shouldEqual Vector(0,1,3) // 013
    Combinations.unrank(3, index=2)  shouldEqual Vector(0,2,3) // 023
    Combinations.unrank(3, index=3)  shouldEqual Vector(1,2,3) // 123
    Combinations.unrank(3, index=4)  shouldEqual Vector(0,1,4) // 014
    Combinations.unrank(3, index=5)  shouldEqual Vector(0,2,4) // 024
    Combinations.unrank(3, index=6)  shouldEqual Vector(1,2,4) // 124
    Combinations.unrank(3, index=7)  shouldEqual Vector(0,3,4) // 034
    Combinations.unrank(3, index=8)  shouldEqual Vector(1,3,4) // 134
    Combinations.unrank(3, index=9)  shouldEqual Vector(2,3,4) // 234
    Combinations.unrank(3, index=10) shouldEqual Vector(0,1,5) // 015
    Combinations.unrank(3, index=11) shouldEqual Vector(0,2,5) // 025
    Combinations.unrank(3, index=12) shouldEqual Vector(1,2,5) // 125
    Combinations.unrank(3, index=13) shouldEqual Vector(0,3,5) // 035
    Combinations.unrank(3, index=14) shouldEqual Vector(1,3,5) // 135
    Combinations.unrank(3, index=15) shouldEqual Vector(2,3,5) // 235
    Combinations.unrank(3, index=16) shouldEqual Vector(0,4,5) // 045
    Combinations.unrank(3, index=17) shouldEqual Vector(1,4,5) // 145
    Combinations.unrank(3, index=18) shouldEqual Vector(2,4,5) // 245
    Combinations.unrank(3, index=19) shouldEqual Vector(3,4,5) // 345
  }

  it should "provide the correct next combination" in {
    Combinations.next(Vector(0)) shouldEqual Vector(1)
    Combinations.next(Vector(1)) shouldEqual Vector(2)

    Combinations.next(Vector(0,10)) shouldEqual Vector(1,10)
    Combinations.next(Vector(9,10)) shouldEqual Vector(0,11)

    Combinations.next(Vector(0,1,2)) shouldEqual Vector(0,1,3)
    Combinations.next(Vector(0,1,3)) shouldEqual Vector(0,2,3)
    Combinations.next(Vector(0,2,3)) shouldEqual Vector(1,2,3)
    Combinations.next(Vector(1,2,3)) shouldEqual Vector(0,1,4)
    Combinations.next(Vector(0,1,4)) shouldEqual Vector(0,2,4)
    Combinations.next(Vector(0,2,4)) shouldEqual Vector(1,2,4)
    Combinations.next(Vector(1,2,4)) shouldEqual Vector(0,3,4)
    Combinations.next(Vector(0,3,4)) shouldEqual Vector(1,3,4)
    Combinations.next(Vector(1,3,4)) shouldEqual Vector(2,3,4)
    Combinations.next(Vector(2,3,4)) shouldEqual Vector(0,1,5)
    Combinations.next(Vector(0,1,5)) shouldEqual Vector(0,2,5)
    Combinations.next(Vector(0,2,5)) shouldEqual Vector(1,2,5)
    Combinations.next(Vector(1,2,5)) shouldEqual Vector(0,3,5)
    Combinations.next(Vector(0,3,5)) shouldEqual Vector(1,3,5)
    Combinations.next(Vector(1,3,5)) shouldEqual Vector(2,3,5)
    Combinations.next(Vector(2,3,5)) shouldEqual Vector(0,4,5)
    Combinations.next(Vector(0,4,5)) shouldEqual Vector(1,4,5)
    Combinations.next(Vector(1,4,5)) shouldEqual Vector(2,4,5)
    Combinations.next(Vector(2,4,5)) shouldEqual Vector(3,4,5)
    Combinations.next(Vector(3,4,5)) shouldEqual Vector(0,1,6)
  }

  it should "provide an iterator for the combination" in {

    val result = Combinations(6,3).iterator().toVector

    result shouldEqual Vector(
      Vector(0,1,2), Vector(0,1,3), Vector(0,2,3), Vector(1,2,3), Vector(0,1,4),
      Vector(0,2,4), Vector(1,2,4), Vector(0,3,4), Vector(1,3,4), Vector(2,3,4),
      Vector(0,1,5), Vector(0,2,5), Vector(1,2,5), Vector(0,3,5), Vector(1,3,5),
      Vector(2,3,5), Vector(0,4,5), Vector(1,4,5), Vector(2,4,5), Vector(3,4,5)
    )
  }

  it should "provide an iterator for a section of the combination" in {
    Combinations(6, 3).range(3, 8).toVector shouldEqual Vector(
      Vector(1,2,3), Vector(0,1,4), Vector(0,2,4), Vector(1,2,4), Vector(0,3,4)
    )
    Combinations(6, 3).range(17, 20).toVector shouldEqual Vector(
      Vector(1,4,5), Vector(2,4,5), Vector(3,4,5)
    )
    Combinations(1, 1).range(0, 1).toVector shouldEqual Vector(Vector(0))
    Combinations(2, 1).range(0, 2).toVector shouldEqual Vector(Vector(0), Vector(1))
    Combinations(2, 1).range(1, 2).toVector shouldEqual Vector(Vector(1))
    Combinations(9, 4).range(116, 126).toVector shouldEqual Vector(
      Vector(1,5,7,8), Vector(2,5,7,8), Vector(3,5,7,8), Vector(4,5,7,8), Vector(0,6,7,8),
      Vector(1,6,7,8), Vector(2,6,7,8), Vector(3,6,7,8), Vector(4,6,7,8), Vector(5,6,7,8)
    )
  }

  it should "loop the iterator on a range that exceeds the combination" in {

    val result1 = Combinations(6,3).range(10, 45).toVector
    result1 shouldEqual Vector(
      Vector(0,1,5), Vector(0,2,5), Vector(1,2,5), Vector(0,3,5), Vector(1,3,5),
      Vector(2,3,5), Vector(0,4,5), Vector(1,4,5), Vector(2,4,5), Vector(3,4,5),

      Vector(0,1,2), Vector(0,1,3), Vector(0,2,3), Vector(1,2,3), Vector(0,1,4),
      Vector(0,2,4), Vector(1,2,4), Vector(0,3,4), Vector(1,3,4), Vector(2,3,4),
      Vector(0,1,5), Vector(0,2,5), Vector(1,2,5), Vector(0,3,5), Vector(1,3,5),
      Vector(2,3,5), Vector(0,4,5), Vector(1,4,5), Vector(2,4,5), Vector(3,4,5),

      Vector(0,1,2), Vector(0,1,3), Vector(0,2,3), Vector(1,2,3), Vector(0,1,4)
    )

    val result2 = Combinations(5,2).range(21, 32).toVector
    result2 shouldEqual Vector(
      Vector(0,2), Vector(1,2), Vector(0,3), Vector(1,3), Vector(2,3), Vector(0,4), Vector(1,4), Vector(2,4), Vector(3,4), Vector(0,1),
      Vector(0,2)
    )
  }
}
