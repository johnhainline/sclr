package combinations

import scala.collection.mutable

class CombinationAggregation(val combinations: Vector[CombinationBuilder]) {
  val count = combinations.foldLeft(BigInt(1))((total, combo) => total * combo.count)
  private def aggregateCount = count

  def all(): Iterator[Vector[Combination]] = {
    new CombinationsAggregationIterator(combinations)
  }

  /**
    * The range is zero-indexed and includes Combinations at index i until j.
    */
  def rangeUntil(i: BigInt, j: BigInt): Iterator[Vector[Combination]] = {
    new CombinationsAggregationIterator(combinations, i, j)
  }

  private class CombinationsAggregationIterator(val combinations: Vector[CombinationBuilder], i: BigInt = 0, j: BigInt = count) extends Iterator[Vector[Combination]] {

    // For each Combinations instance, we get the number we must divide by to remove the influence of all later
    // Combinations. This is kind of the "base" of the combination.
    private val bases = combinations.foldRight(Vector(BigInt(1))){ case (combo, vector) =>
      combo.count * vector.head +: vector
    }.tail

    private var iterators: Vector[Iterator[Combination]] = combinations.zipWithIndex.map { case(combo, index) =>
      val base = bases(index)
      val startIndex = i / base
      val stopIndex = ((j-1) / base) + 1
      combo.rangeUntil(startIndex, stopIndex)
    }
    override def hasNext: Boolean = iterators.last.hasNext

    private val valueStack: mutable.Stack[Combination] = new mutable.Stack().push(Vector(0)) // an initial useless value
    override def next(): Vector[Combination] = {
      // Always pop the first value, as it always changes.
      var last = valueStack.pop()
      while(valueStack.nonEmpty && last == combinations(valueStack.length).last) {
        last = valueStack.pop()
      }
      while(valueStack.length < combinations.length) {
        valueStack.push(iterators(valueStack.length).next())
      }
      valueStack.toVector.reverse
    }
  }
}

object CombinationAggregation {
  def rank(combinations: Vector[Combination]): Vector[(Int, BigInt)] = {
    combinations.map(combo => (combo.length, CombinationBuilder.rank(combo)))
  }

  def unrank(kIndex: Vector[(Int, BigInt)]): Vector[Combination] = {
    kIndex.map(kIndex => CombinationBuilder.unrank(kIndex._1, kIndex._2))
  }
}
