package combinations

import java.math.{MathContext, RoundingMode}

import combinations.iterators.{CombinationsIterator, SamplingIterator, SubsetIterator}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.Random

/**
  * Notes: All combinations return values that are zero indexed. So the first combination of 5 choose 2 is List(0,1).
  * Everything is colexicographic ordering. See (https://oeis.org/wiki/Orderings#Colexicographic_order)
  * Notes:
  * https://blog.plover.com/math/choose.html
  * https://blog.plover.com/math/choose-2.html
  * @param n the total number of choices
  * @param k how many of our choices to take
  */
case class Combinations(n: Int, k: Int) {
  val size: BigInt = Combinations.choose(n, k)

  // Returns an iterator for the complete set of n choose k combinations.
  def iterator(): Iterator[Combination] = {
    new CombinationsIterator(n, k, 0, size, size)
  }

  // Returns an iterator for a subsampling of the complete set of 'n choose k' combinations.
  def samplingIterator(sample: Int, r: Random = new Random()): SamplingIterator = {
    new SamplingIterator(n, k, 0, size, sample, r)
  }

  // Returns an iterator for a subset of indices of n, which have 'subsetSize choose k' combinations.
  def subsetIterator(subsetSize: Int, r: Random = new Random()): SubsetIterator = {
    new SubsetIterator(n, k, 0, Combinations.choose(subsetSize, k), subsetSize, r)
  }

  /** The range is zero-indexed and includes Combinations at index i until j. */
  def range(i: BigInt, j: BigInt): Iterator[Combination] = {
    new CombinationsIterator(n, k, i, j, size)
  }

  /** The range is zero-indexed and includes Combinations at index i until j. */
  def samplingRange(i: BigInt, j: BigInt, sample: Int, r: Random = new Random()): Iterator[Combination] = {
    new SamplingIterator(n, k, i, j, sample, r)
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
  private val chooseCache = new mutable.HashMap[(Int, Int), BigDecimal]()
  private def chooseIterative(n: Int, k: Int): BigDecimal = {
    chooseCache.getOrElseUpdate((n,k), (0 until k).foldLeft(BigDecimal(1.0))((total, i) => total * BigDecimal(n-i) / BigDecimal(k-i)))
  }

  def rank(combination: Combination): BigInt = {
    combination.zipWithIndex.foldLeft(BigInt(0)) {
      case (result, (bit, index)) =>
        result + Combinations.choose(bit, index + 1)
    }
  }

//  def unrank(k: Int, index: BigInt): Combination = {
//    var m = index
//    val result = new mutable.ListBuffer[Int]()
//    for (i <- k to 1 by -1) {
//      var l = i-1
//      while (Combinations.choose(l, i) <= m) {
//        l += 1
//      }
//      result.prepend(l-1)
//      m -= Combinations.choose(l-1, i)
//    }
//    result.toVector
//  }

  def unrank(k: Int, index: BigInt): Combination = {
    var m = index
    val result = new mutable.ListBuffer[Int]()
    for (i <- k to 1 by -1) {
      val l = Combinations.findLargestN(i, m, i-1, Int.MaxValue)
      result.prepend(l)
      m -= Combinations.choose(l, i)
    }
    result.toVector
  }

  /*  We know that for
    *   1 <= k <= n:
    *   (n/k)^k <= (n k) <= (ne/k)^k
    *   However our index "a" can be anything in the range k-1 to (n k).
    *   so given (n k) = a, our max possible n is k/e * a^(1/k)
    *   whereas our min possible n is k-1.
    */
  val floorMC = new MathContext(5, RoundingMode.FLOOR)
  val ceilMC  = new MathContext(5, RoundingMode.CEILING)
  def boundsOfNGivenIndex(k: Int, a: BigInt):(Int, Int) = {
    val high = BigDecimal(k) * nthRoot(k, BigDecimal(a)) * BigDecimal(Math.E)
    (k-1, high.round(ceilMC).toInt + k)
  }

  // Start at n=k-1
  // Find largest n, such that Combinations.choose(n, k) <= index, then return n-1
  // We assume that smallest n is between [low, high].
  @tailrec
  def findLargestN(k: Int, index: BigInt, low: Int, high: Int): Int = {
    if (low == high) {
      low
    } else {
      val middle = ((high + low) / 2.0).ceil.toInt
      val (newLow, newHigh) = if (Combinations.choose(middle, k) <= index) (middle, high) else (low, middle-1)
      findLargestN(k, index, newLow, newHigh)
    }
  }

  private def nthRoot(n: Int, y: BigDecimal): BigDecimal = {
    if (n > 10) {
      nthRoot_bsearch(n, y)
    } else {
      nthRoot_newton(n, y)
    }
  }

  private def nthRoot_newton(n: Int, y: BigDecimal): BigDecimal = {
    @tailrec
    def loop(x0: BigDecimal) : BigDecimal = {
      val x1 = BigDecimal(1.0/n) * ((n - 1) * x0 + y/x0.pow(n-1))
      if (x0 <= x1) x0
      else loop(x1)
    }
    if (y == 0) {
      0
    } else {
      loop(y/2)
    }
  }

  private def nthRoot_bsearch(n: Int, x: BigDecimal): BigDecimal = {
    var low = BigDecimal(0)
    var high = x
    var middle = (high+low) / 2
    var result = middle.pow(n)

    while (result != x) {
      middle = (high+low) / 2
      result = middle.pow(n)
      if (middle == low || middle == high) {
        return middle
      }
      if (result > x) {
        high = middle
      } else {
        low = middle
      }
    }
    middle
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
