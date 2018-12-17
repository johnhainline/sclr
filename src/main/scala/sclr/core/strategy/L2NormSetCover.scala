package sclr.core.strategy

import combinations.Combinations
import sclr.core.Messages.{Work, Workload}
import sclr.core.database.{Dataset, Result, XYZ}

import scala.collection.immutable.BitSet
import scala.collection.mutable.ListBuffer

object L2NormSetCover {

  case class Term(bitset: BitSet, i1: Int, i2: Int)

  private def createBitsetForBooleanSelections(data: Array[XYZ], selections: Vector[(Int, Boolean)]): BitSet = {
    val mapFilter = new PartialFunction[XYZ, Int] {
      def apply(xyz: XYZ): Int = xyz.id

      def isDefinedAt(xyz: XYZ): Boolean = selections.map(selection => xyz.x(selection._1) == selection._2).forall(identity)
    }
    val ids = data.collect(mapFilter)
    BitSet(ids: _*)
  }

  private def createAllTerms(dataset: Dataset, n: Int, k: Int): List[Term] = {
    Combinations(n, k).iterator().foldLeft(new ListBuffer[Term]()) { case (terms: ListBuffer[Term], indices: combinations.Combination) =>
      val (a, b) = (indices(0) + 1, indices(1) + 1)
      val indicesForIndex = List((a, b), (-a, b), (a, -b), (-a, -b))
      for (i <- indicesForIndex) {
        val bitset = createBitsetForBooleanSelections(dataset.data, Vector((Math.abs(i._1) - 1, i._1 > 0), (Math.abs(i._2) - 1, i._2 > 0)))
        terms.append(Term(bitset, i._1, i._2))
      }
      terms
    }.toList
  }

  private def coefficientsValid(coeff1: Double, coeff2: Double): Boolean = {
    (coeff1 != 0 && coeff2 != 0) &&
      (!coeff1.isNaN && coeff1 > Double.NegativeInfinity && coeff1 < Double.PositiveInfinity) &&
      (!coeff2.isNaN && coeff2 > Double.NegativeInfinity && coeff2 < Double.PositiveInfinity)
  }

  private def calculateCoefficients(data: Array[XYZ], work: Work): (Double, Double) = {
    val yDimensions = work.selectedDimensions
    val rows = work.selectedRows
    val xyz1 = data(rows(0))
    val xyz2 = data(rows(1))

    val x1 = xyz1.y(yDimensions(0))
    val y1 = xyz1.y(yDimensions(1))
    val z1 = xyz1.z

    val x2 = xyz2.y(yDimensions(0))
    val y2 = xyz2.y(yDimensions(1))
    val z2 = xyz2.z

    val a1 = (z1 * y2 - z2 * y1) / (x1 * y2 - x2 * y1)
    val a2 = (x1 * z2 - x2 * z1) / (x1 * y2 - x2 * y1)
    (a1, a2)
  }

  private def createIdToRednessMap(data: Array[XYZ], yDimensions: Array[Int], coeff1: Double, coeff2: Double): Map[Int, Double] = {
    data.map { xyz =>
      val redness = Math.pow(xyz.z - coeff1 * xyz.y(yDimensions(0)) - coeff2 * xyz.y(yDimensions(1)), 2)
      (xyz.id, redness)
    }.toMap
  }

  private def termsSortedByAverageRedness(idToRedness: Map[Int, Double], terms: List[Term]): Array[(Term, Double)] = {
    val array = new Array[(Term, Double)](terms.length)
    for (i <- terms.indices) {
      val term = terms(i)
      val termTotalRedness = term.bitset.iterator.map(idToRedness).sum
      val termAverageRedness = termTotalRedness / term.bitset.size.toDouble
      array.update(i, (term, termAverageRedness))
    }
    scala.util.Sorting.quickSort(array)(Ordering.by[(Term, Double), Double](_._2))
    array
  }

  private def termsUnionAsBitset(terms: Seq[Term]): BitSet = {
    if (terms.isEmpty) {
      BitSet.empty
    } else {
      terms.foldLeft(BitSet.empty)((accum, term) => accum.union(term.bitset))
    }
  }

  private def errorRate(idToRedness: Map[Int, Double], terms: Seq[Term]): Double = {
    val bitset = termsUnionAsBitset(terms)
    val totalRedness = bitset.foldLeft(0.0)((accumulator, term) => accumulator + idToRedness(term))
    val averageRedness = totalRedness / bitset.size.toDouble
    if (averageRedness.isNaN) Double.MaxValue else averageRedness
  }
}

class L2NormSetCover(val dataset: Dataset, val workload: Workload) extends KDNFStrategy {
  import L2NormSetCover._

  val terms = createAllTerms(dataset, dataset.xLength, workload.dnfSize)

  def run(work: Work): Result = {
    val (coeff1, coeff2) = calculateCoefficients(dataset.data, work)
    if (coefficientsValid(coeff1, coeff2)) {
      val idToRedness = createIdToRednessMap(dataset.data, work.selectedDimensions, coeff1, coeff2)
      val sortedTerms = termsSortedByAverageRedness(idToRedness, terms)
      val idCount = dataset.data.length
      val totalRedness = idToRedness.values.sum

      var minError = Double.MaxValue
      var bestTerms = List.empty[Term]
      val currentTerms = ListBuffer[Term]()
      var i = 0
      while (workload.mu * idCount - termsUnionAsBitset(currentTerms).size > 0) {
        currentTerms.append(sortedTerms(i)._1)
        i += 1
      }
      val newError = errorRate(idToRedness, currentTerms)
      if (minError > newError) {
        minError = newError
        bestTerms = currentTerms.toList
      }
      val kdnfString = bestTerms.map(term => s"(${term.i1}, ${term.i2})").mkString
      val kdnf = if (bestTerms.nonEmpty) Some(kdnfString) else None
      Result(work.index, work.selectedDimensions, work.selectedRows, Array(coeff1, coeff2), Some(minError), kdnf)
    } else {
      Result(work.index, work.selectedDimensions, work.selectedRows, Array(0, 0), None, None)
    }
  }
}
