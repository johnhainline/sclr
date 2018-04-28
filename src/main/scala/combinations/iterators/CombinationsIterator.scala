package combinations.iterators

import combinations.{Combination, Combinations}

class CombinationsIterator(n: Int, k: Int, i: BigInt, j: BigInt, size: BigInt) extends Iterator[Combination] {
  private var currentIndex: BigInt = i
  private var current: Combination = if (i == 0) Combinations.first(n, k) else Combinations.unrank(k, i % size)

  override def hasNext: Boolean = currentIndex < j

  override def next(): Combination = {
    val previous = current
    currentIndex += 1
    if (currentIndex % size == 0) {
      current = Combinations.first(n, k)
    } else {
      current = Combinations.next(current)
    }
    previous
  }
}
