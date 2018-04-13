package combinations

import org.scalatest._

class MultipliedIteratorSpec extends FlatSpec with Matchers {

  "CombinationsAggregation" should "hold the total count of combos" in {
    val test1 = MultipliedIterator(Vector(() => Combinations(10,2).iterator(), () => Combinations(3,1).iterator())).toVector
    test1.length shouldEqual 135

    val test2 = MultipliedIterator(Vector(() => Combinations(5,2).iterator(), () => Combinations(6,2).iterator())).toVector
    test2.length shouldEqual 150
  }

  // For 6 choose 3 -> {0,1,3,3,4,5}
  it should "use correct rank" in {
    Combinations.rank(Vector(Vector(0), Vector(1))) shouldEqual Vector((1,0), (1,1))
    Combinations.rank(Vector(Vector(0,1,2), Vector(0,3,4,5))) shouldEqual Vector((3,0), (4,12))
    Combinations.rank(Vector(Vector(2,3,5), Vector(0,1,2), Vector(8,0))) shouldEqual Vector((3,15), (3,0), (2,8))
  }

  it should "use correct unrank" in {
    Combinations.unrank(Vector((3, 7), (2, 0))) shouldEqual Vector(Vector(0,3,4), Vector(0,1))
    Combinations.unrank(Vector((3,16), (3,10))) shouldEqual Vector(Vector(0,4,5), Vector(0,1,5))
  }

  it should "provide an iterator for the combination" in {

    val result = MultipliedIterator(Vector(() => Combinations(4,3).iterator(), () => Combinations(5,2).iterator(), () => Combinations(2,1).iterator())).toVector

    val combo1 = Combinations(4,3).iterator().toVector
    val combo2 = Combinations(5,2).iterator().toVector
    val combo3 = Combinations(2,1).iterator().toVector
    val expected = for (a <- combo1; b <- combo2; c <- combo3) yield {
      Vector(a,b,c)
    }
    result shouldEqual expected
  }

  it should "provide an iterator for a section of combinations" in {

    val result1 = MultipliedIterator(Vector(() => Combinations(3,2).range(0,1), () => Combinations(2,1).range(0,1), () => Combinations(5,2).range(3,5))).toVector
    val expected1 = Vector(
//      Vector(Vector(0,1),Vector(0),Vector(0,1)), // 0
//      Vector(Vector(0,1),Vector(0),Vector(0,2)), // 1
//      Vector(Vector(0,1),Vector(0),Vector(1,2)), // 2
      Vector(Vector(0,1),Vector(0),Vector(0,3)), // 3
      Vector(Vector(0,1),Vector(0),Vector(1,3))  // 4
    )
    result1 shouldEqual expected1

    val result2 = MultipliedIterator(Vector(() => Combinations(3,2).range(1,2), () => Combinations(2,1).range(0,1), () => Combinations(5,2).iterator())).toVector
    val expected2 = Vector(
//      Vector(Vector(0,1),Vector(1),Vector(3,4)), // 19
      Vector(Vector(0,2),Vector(0),Vector(0,1)), // 20
      Vector(Vector(0,2),Vector(0),Vector(0,2)), // 21
      Vector(Vector(0,2),Vector(0),Vector(1,2)), // 22
      Vector(Vector(0,2),Vector(0),Vector(0,3)), // 23
      Vector(Vector(0,2),Vector(0),Vector(1,3)), // 24
      Vector(Vector(0,2),Vector(0),Vector(2,3)), // 25
      Vector(Vector(0,2),Vector(0),Vector(0,4)), // 26
      Vector(Vector(0,2),Vector(0),Vector(1,4)), // 27
      Vector(Vector(0,2),Vector(0),Vector(2,4)), // 28
      Vector(Vector(0,2),Vector(0),Vector(3,4))  // 29
//      Vector(Vector(0,2),Vector(1),Vector(0,1)), // 30
//      Vector(Vector(0,2),Vector(1),Vector(0,2))  // 31
    )
    result2 shouldEqual expected2
  }
}
