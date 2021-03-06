package sclr.core.strategy

import sclr.core.Messages.{Work, Workload}
import sclr.core.database._
import combinations.Combinations

import scala.collection.immutable.BitSet

class L2Norm(val dataset: Dataset, val workload: Workload, simpleAlgorithm: Boolean = true) extends KDNFStrategy {

  // Construct all possible dnf set memberships.
  val termToIndices: Map[BitSet, (Int, Int)] =
    Combinations(dataset.xLength, workload.dnfSize).iterator().flatMap { zeroIndexedIndices =>
    // Our indices are 0-indexed, so increase that to 1-indexed to allow negative indices.
    //      val oneIndexedIndices = zeroIndexedIndices.map(_+1)
    val (a, b) = (zeroIndexedIndices(0) + 1, zeroIndexedIndices(1) + 1)
    val combinations = Vector((a, b), (-a, b), (a, -b), (-a, -b))
    val result = combinations.map { case (i1, i2) =>
      // Construct the BitSet that represents this particular index pair
      val filteredPoints = collectFilter(dataset.data, Vector((Math.abs(i1) - 1, i1 > 0), (Math.abs(i2) - 1, i2 > 0)))
      (filteredPoints, (i1, i2))
    }
    result
  }.toMap
  val terms: Vector[BitSet] = termToIndices.keys.toVector
  val setCover = new SetCover(terms, workload.mu, dataset.xLength, simpleAlgorithm = simpleAlgorithm)

  def run(work: Work): Result = {
    val (a1, a2) = generateCoefficients(dataset.data, work)
    if (coefficientsValid(a1, a2)) {
      // Construct redness score with a map of XYZ.id -> redness
      val idToRedness = constructRednessScores(dataset.data, work.selectedDimensions, a1, a2)
      val (kdnfTerms, error) = setCover.lowDegPartial2(idToRedness)
      val kdnfString = kdnfTerms.map(termToIndices).toString
      val kdnf = if (kdnfTerms.nonEmpty) Some(kdnfString) else None
      Result(work.index, work.selectedDimensions, work.selectedRows, Array(a1, a2), Some(error), kdnf)
    } else {
      Result(work.index, work.selectedDimensions, work.selectedRows, Array(0, 0), None, None)
    }
  }

  private def coefficientsValid(coeff1: Double, coeff2: Double): Boolean = {
    (coeff1 != 0 && coeff2 != 0) &&
    (!coeff1.isNaN && coeff1 > Double.NegativeInfinity && coeff1 < Double.PositiveInfinity) &&
    (!coeff2.isNaN && coeff2 > Double.NegativeInfinity && coeff2 < Double.PositiveInfinity)
  }

  private def generateCoefficients(data: Array[XYZ], work: Work): (Double, Double) = {
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

  private def constructRednessScores(data: Array[XYZ], yDimensions: Array[Int], a1: Double, a2: Double): Map[Int, Double] = {
    val idToRedness = if (!coefficientsValid(a1, a2)) {
      Map.empty[Int, Double]
    } else {
      data.map { xyz =>
        val redness = Math.pow(xyz.z - a1 * xyz.y(yDimensions(0)) - a2 * xyz.y(yDimensions(1)), 2)
        (xyz.id, redness)
      }.toMap
    }
    idToRedness
  }

  private def collectFilter(data: Array[XYZ], selections: Vector[(Int, Boolean)]): BitSet = {
    val mapFilter = new PartialFunction[XYZ, Int] {
      def apply(xyz: XYZ): Int = xyz.id
      def isDefinedAt(xyz: XYZ): Boolean = selections.map(selection => xyz.x(selection._1) == selection._2).forall(identity)
    }
    val ids = data.collect(mapFilter)
    BitSet(ids:_*)
  }
}
