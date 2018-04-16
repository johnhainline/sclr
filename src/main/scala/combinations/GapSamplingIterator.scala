package combinations

import scala.reflect.ClassTag
import scala.util.Random

/** See: https://erikerlandson.github.io/blog/2014/09/11/faster-random-samples-with-gap-sampling/
  * Taken from: https://gist.github.com/erikerlandson/05db1f15c8d623448ff6
  */
class GapSamplingIterator[T: ClassTag] private (var data: Iterator[T], p: Double, r: Random) extends Iterator[T] {

  // implement efficient drop until scala includes fix for jira SI-8835
  val dd: Int => Unit = {
    val arrayClass = Array.empty[T].iterator.getClass
    data.getClass match {
      case `arrayClass` =>
        (n: Int) => { data = data.drop(n) }
      case _ =>
        (n: Int) => {
          var j = 0
          while (j < n && data.hasNext) {
            data.next
            j += 1
          }
        }
    }
  }

  override def hasNext: Boolean = data.hasNext

  override def next: T = {
    val r = data.next
    advance
    r
  }

  private val lnq = Math.log(1.0 - p)

  def advance() = {
    // skip elements with replication factor zero (i.e. elements that won't be sampled)
    val u = Math.max(r.nextDouble(), 1e-10)
    val rlen = (Math.log(u) / lnq).toInt
    dd(rlen)
  }

  // advance to first sample as part of object construction
  advance()
}

object GapSamplingIterator {
  def apply[T: ClassTag](data: Iterator[T], p: Double): GapSamplingIterator[T] = {
    new GapSamplingIterator[T](data, p, new Random())
  }

  def apply[T: ClassTag](data: Iterator[T], p: Double, r: Random): GapSamplingIterator[T] = {
    new GapSamplingIterator[T](data, p, r)
  }
}
