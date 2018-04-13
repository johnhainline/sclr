package combinations

import scala.collection.mutable

/**
  * Notes: All combinations return values that are zero indexed. So the first combination of 5 choose 2 is List(0,1).
  * Everything is colexicographic ordering. See (https://oeis.org/wiki/Orderings#Colexicographic_order)
  * @param n the total number of choices
  * @param k how many of our choices to take
  */
case class Combinations(val n: Int, val k: Int) {
  val size: BigInt = Combinations.choose(n, k)

  // Returns an iterator for the complete set of n choose k combinations.
  def iterator(): Iterator[Combination] = {
    new CombinationsIterator(n, k, 0, size, size)
  }

  /**
    * The range is zero-indexed and includes Combinations at index i until j.
    */
  def range(i: BigInt, j: BigInt): Iterator[Combination] = {
    new CombinationsIterator(n, k, i, j, size)
  }
}

object Combinations {
  def first(n: Int, k: Int): Combination = {
    val first = for (i <- 0 until k) yield i
    first.toVector
  }

  def last(n: Int, k: Int): Combination = {
    val last = for (i <- n-k until n) yield i
    last.toVector
  }

  def choose(n: Int, k: Int): BigInt = {
    if (n < k) return BigInt(0)
    val lowK = if (n - k < k) n - k else k
    chooseIterative(n, lowK).toBigInt()
  }
  private def chooseIterative(n: Int, k: Int): BigDecimal = {
    (0 until k).foldLeft(BigDecimal(1.0))((total, i) => total * BigDecimal(n-i) / BigDecimal(k-i))
  }

  def rank(combination: Combination): BigInt = {
    combination.zipWithIndex.foldLeft(BigInt(0)) {
      case (result, (bit, index)) =>
        result + Combinations.choose(bit, index + 1)
    }
  }

  def unrank(k: Int, index: BigInt): Combination = {
    var m = index
    val result = new mutable.ListBuffer[Int]()
    for (i <- k to 1 by -1) {
      var l = i-1
      while (Combinations.choose(l, i) <= m) {
        l += 1
      }
      result.prepend(l-1)
      m -= Combinations.choose(l-1, i)
    }
    result.toVector
  }

  def rank(combinations: Vector[Combination]): Vector[(Int, BigInt)] = {
    combinations.map(combo => (combo.length, Combinations.rank(combo)))
  }

  def unrank(kIndex: Vector[(Int, BigInt)]): Vector[Combination] = {
    kIndex.map(kIndex => Combinations.unrank(kIndex._1, kIndex._2))
  }

  def next(combination: Combination): Combination = {
    val result = mutable.ListBuffer[Int]()
    // Find the largest index c(i) such that c(i+1) > c(i)+1 (or use the last index).
    val index = findIndex(combination)
    for (i <- combination.indices) {
      if (i < index) {
        result.append(i)
      } else if (i == index) {
        result.append(combination(i) + 1)
      } else {
        result.append(combination(i))
      }
    }
    result.toVector
  }

  private def findIndex(combination: Combination): Int = {
    var index = 0
    if (combination.length > 1) {
      var foundIndex = false
      while (!foundIndex && index < combination.length-1) {
        val current = combination(index)
        val next = combination(index+1)
        if (current + 1 < next) {
          foundIndex = true
        } else {
          index += 1
        }
      }
      if (!foundIndex) {
        index = combination.length - 1
      }
    }
    index
  }
}
