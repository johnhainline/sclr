package combinations

case class CombinationAggregation(combinations: Vector[CombinationBuilder]) {
  val count: BigInt = combinations.foldLeft(BigInt(1))((total, combo) => total * combo.count)

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

    private val iterators: Vector[Iterator[Combination]] = combinations.zipWithIndex.map { case(combo, index) =>
      val base = bases(index)
      val startIndex = i / base
      val stopIndex = ((j-1) / base) + 1
      combo.rangeUntil(startIndex, stopIndex)
    }
    override def hasNext: Boolean = iterators.last.hasNext

    private var valueStack: List[Combination] = List(Vector(0)) // an initial useless value
    private def stackPop() = {
      val head = valueStack.head
      valueStack = valueStack.tail
      head
    }
    private def stackPush(combination: Combination): Unit = {
      valueStack = combination :: valueStack
    }

    override def next(): Vector[Combination] = {
      // Always pop the first value, as it always changes.
      var last = stackPop()
      while(valueStack.nonEmpty && last == combinations(valueStack.length).last) {
        last = stackPop()
      }
      while(valueStack.lengthCompare(combinations.length) < 0) {
        stackPush(iterators(valueStack.length).next())
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
