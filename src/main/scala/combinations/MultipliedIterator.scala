package combinations

import scala.annotation.tailrec
import scala.reflect.ClassTag

/**
  * Takes a sequence of iterator generator functions. Generates an iterator from each generator. The first iterator
  * returned is special and only gets iterated once. Every other iterator is fully iterated for each previous iterator
  * value. So we're kind of "multiplying" the iterators together.
  */
class MultipliedIterator[T: ClassTag] private(iteratorGenerators: Vector[() => Iterator[T]]) extends Iterator[Vector[T]] {

  private val iterators: Array[Iterator[T]] = iteratorGenerators.map(ig => ig().buffered).toArray
  assert(iteratorGenerators.nonEmpty)
  assert(iterators.forall(_.hasNext))
  private val values: Array[T] = new Array[T](iterators.length)
  for (i <- 0 to iterators.length - 2)
    values(i) = iterators(i).next()

  override def hasNext: Boolean = iterators.exists(_.hasNext)
  override def next(): Vector[T] = {
    iterate(iterators.length - 1)
    values.toVector
  }

  private def updateIterator(index: Int): Unit = {
    values(index) = iterators(index).next()
  }

  @tailrec
  private def iterate(index: Int): Unit = {
    if (iterators(index).hasNext) {
      updateIterator(index)
    } else if (index != 0) {
      // For every iterator (except the first), rebuild it if it ran out of items.
      iterators(index) = iteratorGenerators(index)()
      updateIterator(index)
      iterate(index-1)
    }
  }
}
object MultipliedIterator {
  def apply[T: ClassTag](iteratorGenerators: Vector[() => Iterator[T]]): MultipliedIterator[T] = {
    new MultipliedIterator[T](iteratorGenerators)
  }
}