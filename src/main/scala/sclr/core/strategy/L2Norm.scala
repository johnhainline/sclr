package sclr.core.strategy

import sclr.core.Messages.{Work, Workload}
import sclr.core.database._
import combinations.Combinations

import scala.collection.immutable.BitSet

class L2Norm(val dataset: Dataset, val workload: Workload, simpleAlgorithm: Boolean = true) extends KDNFStrategy(dataset) {

  // Construct all possible dnf set memberships.
  lazy val termToIndices = Combinations(dataset.xLength, workload.dnfSize).iterator().flatMap { zeroIndexedIndices =>
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
  lazy val terms = termToIndices.keys.toVector
  lazy val setCover = new SetCover(terms, workload.mu, dataset.xLength, simpleAlgorithm = simpleAlgorithm)

  def run(work: Work): Result = {
    // Construct redness score with a map of XYZ.id -> redness
    val (idToRedness, coeff1, coeff2) = constructRednessScores(dataset.data, work)
    val (kdnf, error) = if (coeff1 == 0 && coeff2 == 0) {
      (None, None)
    } else {
      val (kdnfTerms, error) = setCover.lowDegPartial2(idToRedness)
      val kdnfString = kdnfTerms.map(termToIndices).toString
      val kdnf = if (kdnfTerms.nonEmpty) Some(kdnfString) else None
      (kdnf, Some(error))
    }
    Result(work.index, work.selectedDimensions, work.selectedRows, Vector(coeff1, coeff2), error, kdnf)
  }

  private def constructRednessScores(data: Array[XYZ], work: Work) = {
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

    val idToRedness = if (a1 == 0 && a2 == 0) {
      Map.empty[Int, Double]
    } else {
      data.map { xyz =>
        val redness = Math.pow(xyz.z - a1 * xyz.y(yDimensions(0)) - a2 * xyz.y(yDimensions(1)), 2)
        (xyz.id, redness)
      }.toMap
    }
    (idToRedness, a1, a2)
  }

  private def collectFilter(data: Array[XYZ], selections: Vector[(Int, Boolean)]) = {
    val mapFilter = new PartialFunction[XYZ, Int] {
      def apply(xyz: XYZ) = xyz.id
      def isDefinedAt(xyz: XYZ) = selections.map(selection => xyz.x(selection._1) == selection._2).forall(identity)
    }
    val ids = data.collect(mapFilter)
    BitSet(ids:_*)
  }
}
