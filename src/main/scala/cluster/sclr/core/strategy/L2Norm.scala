package cluster.sclr.core.strategy

import cluster.sclr.Messages.Workload
import cluster.sclr.core._
import combinations.CombinationBuilder

import scala.collection.immutable.BitSet

class L2Norm(workload: Workload) extends KdnfStrategy {
  val dnfSize = workload.dnfSize
  val mu = workload.mu

  def run(dataset: Dataset, yDimensions: Vector[Int], rows: Vector[Int]): Option[Result] = {
    val termToIndices = CombinationBuilder(dataset.xLength, dnfSize).all().flatMap { zeroIndexedIndices =>
      // Our indices are 0-indexed, so increase that to 1-indexed to allow negative indices.
      //      val oneIndexedIndices = zeroIndexedIndices.map(_+1)
      val (a, b) = (zeroIndexedIndices(0) + 1, zeroIndexedIndices(1) + 1)
      val combinations = Vector((a, b), (-a, b), (a, -b), (-a, -b))
      val result = combinations.map { case (i1, i2) =>
        // Search for the set of Points that fits this particular index pair
        val filteredPoints = selectBooleanValuesAtIndices(dataset.data, Vector((Math.abs(i1) - 1, i1 > 0), (Math.abs(i2) - 1, i2 > 0)))
        (filteredPoints, (i1, i2))
      }
      result
    }.toMap
    val (idToRedness, coeff1, coeff2) = constructRednessScores(dataset.data, yDimensions, rows)
    val terms = termToIndices.keys.toVector
    val (kDNF, error) = new SetCover(terms, idToRedness, mu, dataset.data.length).lowDegPartial2(simpleAlgorithm = true)
    val kDNFString = kDNF.map(termToIndices).toString

    if (kDNF.nonEmpty) {
      Some(Result(yDimensions, rows, Vector(coeff1, coeff2), error, kDNFString))
    } else {
      None
    }
  }

  private def constructRednessScores(data: Array[XYZ], yDimensions: Vector[Int], rows: Vector[Int]) = {
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

    val idToRedness = data.map { xyz =>
      val redness = Math.pow(xyz.z - a1 * xyz.y(yDimensions(0)) - a2 * xyz.y(yDimensions(1)), 2)
      (xyz.id, redness)
    }.toMap
    (idToRedness, a1, a2)
  }

  private def selectBooleanValuesAtIndices(data: Array[XYZ], selections: Vector[(Int, Boolean)]) = {
    val ids = data.filter { xyz =>
      selections.map(selection => xyz.x(selection._1) == selection._2).forall(identity)
    }.map(_.id)
    BitSet(ids:_*)
  }
}
