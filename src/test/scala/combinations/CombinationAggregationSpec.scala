package combinations

import org.scalatest._

class CombinationAggregationSpec extends FlatSpec with Matchers {

  "CombinationsAggregation" should "hold the total count of combos" in {
    CombinationAggregation(Vector(CombinationBuilder(10,2), CombinationBuilder(3,1))).count shouldEqual 135
    CombinationAggregation(Vector(CombinationBuilder(5,2), CombinationBuilder(6,2))).count shouldEqual 150
  }

  // For 6 choose 3 -> {0,1,3,3,4,5}
  it should "use correct rank" in {
    CombinationAggregation.rank(Vector(Vector(0), Vector(1))) shouldEqual Vector((1,0), (1,1))
    CombinationAggregation.rank(Vector(Vector(0,1,2), Vector(0,3,4,5))) shouldEqual Vector((3,0), (4,12))
    CombinationAggregation.rank(Vector(Vector(2,3,5), Vector(0,1,2), Vector(8,0))) shouldEqual Vector((3,15), (3,0), (2,8))
  }

  it should "use correct unrank" in {
    CombinationAggregation.unrank(Vector((3, 7), (2, 0))) shouldEqual Vector(Vector(0,3,4), Vector(0,1))
    CombinationAggregation.unrank(Vector((3,16), (3,10))) shouldEqual Vector(Vector(0,4,5), Vector(0,1,5))
  }

  it should "provide an iterator for the combination" in {

    val result = CombinationAggregation(Vector(CombinationBuilder(4,3), CombinationBuilder(5,2), CombinationBuilder(2,1))).all().toVector

    val combo1 = CombinationBuilder(4,3).all().toVector
    val combo2 = CombinationBuilder(5,2).all().toVector
    val combo3 = CombinationBuilder(2,1).all().toVector
    val expected = for (a <- combo1; b <- combo2; c <- combo3) yield {
      Vector(a,b,c)
    }
    result shouldEqual expected
  }

  it should "provide an iterator for a section of combinations" in {

    val result1 = CombinationAggregation(Vector(CombinationBuilder(3,2), CombinationBuilder(2,1), CombinationBuilder(5,2))).rangeUntil(3,5).toVector
    val expected1 = Vector(
//      Vector(Vector(0,1),Vector(0),Vector(0,1)), // 0
//      Vector(Vector(0,1),Vector(0),Vector(0,2)), // 1
//      Vector(Vector(0,1),Vector(0),Vector(1,2)), // 2
      Vector(Vector(0,1),Vector(0),Vector(0,3)), // 3
      Vector(Vector(0,1),Vector(0),Vector(1,3))  // 4
    )
    result1 shouldEqual expected1

    val result2 = CombinationAggregation(Vector(CombinationBuilder(3,2), CombinationBuilder(2,1), CombinationBuilder(5,2))).rangeUntil(21,32).toVector
    val expected2 = Vector(
      Vector(Vector(0,2),Vector(0),Vector(0,2)), // 21
      Vector(Vector(0,2),Vector(0),Vector(1,2)), // 22
      Vector(Vector(0,2),Vector(0),Vector(0,3)), // 23
      Vector(Vector(0,2),Vector(0),Vector(1,3)), // 24
      Vector(Vector(0,2),Vector(0),Vector(2,3)), // 25
      Vector(Vector(0,2),Vector(0),Vector(0,4)), // 26
      Vector(Vector(0,2),Vector(0),Vector(1,4)), // 27
      Vector(Vector(0,2),Vector(0),Vector(2,4)), // 28
      Vector(Vector(0,2),Vector(0),Vector(3,4)), // 29
      Vector(Vector(0,2),Vector(1),Vector(0,1)), // 30
      Vector(Vector(0,2),Vector(1),Vector(0,2))  // 31
    )
    result2 shouldEqual expected2
  }
}
