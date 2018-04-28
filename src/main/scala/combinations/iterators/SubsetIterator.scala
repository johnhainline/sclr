package combinations.iterators

import combinations.{Combination, Combinations}

import scala.collection.mutable
import scala.util.Random

class SubsetIterator(n: Int, k: Int, i: BigInt, j: BigInt, subsetSize: Int, r: Random) extends Iterator[Combination] {

  private val internalIterator = new CombinationsIterator(subsetSize, k, i, j, Combinations.choose(subsetSize, k))

  private val samples = {
    var tries = 0
    val sampleSet = new mutable.TreeSet[Int]()
    while (tries < 3*subsetSize && sampleSet.size < subsetSize) {
      sampleSet += r.nextInt(n)
      tries += 1
    }
    sampleSet.toArray
  }

  override def size: Int = internalIterator.size
  override def hasNext: Boolean = internalIterator.hasNext
  override def next(): Combination = {
    val result = internalIterator.next()
    result.map(samples)
  }
}
