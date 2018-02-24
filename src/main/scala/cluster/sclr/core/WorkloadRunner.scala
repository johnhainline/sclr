package cluster.sclr.core

import cluster.sclr.core.WorkloadRunner._
import com.typesafe.scalalogging.LazyLogging
import combinations.CombinationBuilder

class WorkloadRunner(dataset: Dataset) extends LazyLogging {

  def run(dnfSize: Int, mu: Double, yDimensions: Vector[Int], rows: Vector[Int]): Option[Result] = {

    val (points, coeff1, coeff2) = WorkloadRunner.constructRednessScores(dataset.data, yDimensions, rows)

    val dnfToIndices = CombinationBuilder(dataset.xLength, dnfSize).all().flatMap { zeroIndexedIndices =>
      // Our indices are 0-indexed, so increase that to 1-indexed to allow negative indices.
//      val oneIndexedIndices = zeroIndexedIndices.map(_+1)
      val (a,b) = (zeroIndexedIndices(0)+1,zeroIndexedIndices(1)+1)
      val combinations = Vector((a, b), (-a, b), (a, -b), (-a, -b))
      val result = combinations.map { case (i1,i2) =>
        // Search for the set of Points that fits this particular index pair
        val filteredPoints = selectBooleanValuesAtIndices(points, Vector((Math.abs(i1)-1, i1 > 0), (Math.abs(i2)-1, i2 > 0)))
        (filteredPoints, (i1,i2))
      }
      result
    }.toMap
    val (kDNF, error) = new SetCover(dnfToIndices.keySet, mu, dataset.data.length).lowDegPartial2(simpleAlgorithm = true)
    val kDNFString = kDNF.map(dnfToIndices).toString

    if (kDNF.nonEmpty) {
      Some(Result(yDimensions, rows, Vector(coeff1, coeff2), error, kDNFString))
    } else {
      None
    }
  }
}

object WorkloadRunner {

  private def constructRednessScores(data: Array[XYZ], yDimensions: Vector[Int], rows: Vector[Int]) = {
    val xyz1 = data(rows(0))
    val xyz2 = data(rows(1))

    val x1 = xyz1.y(yDimensions(0))
    val y1 = xyz1.y(yDimensions(1))
    val z1 = xyz1.z

    val x2 = xyz2.y(yDimensions(0))
    val y2 = xyz2.y(yDimensions(1))
    val z2 = xyz2.z

    val a1 = (z1*y2- z2*y1) / (x1*y2-x2*y1)
    val a2 = (x1*z2- x2*z1) / (x1*y2-x2*y1)

    val points = data.map { xyz =>
      val redness = Math.pow(xyz.z - a1*xyz.y(yDimensions(0)) - a2*xyz.y(yDimensions(1)), 2)
      Point(xyz, redness)
    }
    (points, a1, a2)
  }

  private def selectBooleanValuesAtIndices(points: Array[Point], selections: Vector[(Int, Boolean)]) = {
    points.filter { point =>
      selections.map(selection => point.xyz.x(selection._1) == selection._2).forall(identity)
    }.toSet
  }

}