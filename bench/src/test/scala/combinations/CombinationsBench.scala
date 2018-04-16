package combinations

import org.scalameter.api._
import org.scalameter.picklers.Implicits._

import scala.collection.mutable
import scala.util.Random

object CombinationsBench extends Bench.OfflineReport {

  val random = new Random(1234)
  val genK     = Gen.enumeration(axisName = "k")(1,10,100)
  val genIndex = Gen.range(axisName = "index")(0, 990000, 110000)
  val genK_Index = for(k <- genK; index <- genIndex) yield {
    (k, index)
  }

  performance of "Combinations" config (
    exec.benchRuns -> 100
    ) in {
    measure method "unrank_current" in {
      using(genK_Index) in { case (k, index) =>
        Combinations.unrank(k, index)
      }
    }
    measure method "unrank_new" in {
      using(genK_Index) in { case (k, index) =>
        unrank(k, index)
      }
    }
  }

  def unrank(k: Int, index: BigInt): Combination = {
    var m = index
    val result = new mutable.ListBuffer[Int]()
    for (i <- k to 1 by -1) {
      val (low, high) = Combinations.boundsOfNGivenIndex(k, m)
//      val (low, high) = (k-1, if (m > Int.MaxValue) Int.MaxValue else m.toInt + k)
      val l = Combinations.findLargestN(i, m, low, high)
      result.prepend(l)
      m -= Combinations.choose(l, i)
    }
    result.toVector
  }

}



