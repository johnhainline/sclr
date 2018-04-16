package combinations

import scala.util.Random

/**
  * If we ever decide to implement this taking into account the Birthday problem, then see:
  * https://math.stackexchange.com/questions/31823/collisions-in-a-sample-of-uniform-distribution?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
  */
class SamplingCombinationsIterator(n: Int, k: Int, i: BigInt, j: BigInt, sample: Int, r: Random) extends Iterator[Combination] {

  private val difference = j - i
  private val bitCount = Math.ceil(j.bitLength).toInt + 1
  private val maxGenSample = BigInt(2).pow(bitCount)
  private def genSample(): BigInt = {
    (BigInt(bitCount, r) * difference / maxGenSample) + i
  }
  // We build our sample indices immediately and sort. So we keep this as an ordered iterator.
  private val samples = {
    val s = Range(0, sample).map(_ => genSample()).toArray
    util.Sorting.quickSort(s)
    s.foldRight(List.empty[BigInt]) {
      case (a, b) =>
        if (b.nonEmpty && b.head == a)
          b
        else
          a :: b
    }
  }

  var currentIndex = 0
  override def hasNext: Boolean = currentIndex < samples.length

  override def next(): Combination = {
    val result = Combinations.unrank(k, samples(currentIndex))
    currentIndex += 1
    result
  }
}
