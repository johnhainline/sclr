package cluster.sclr.core

import cluster.sclr.core.WorkloadRunner._
import com.typesafe.scalalogging.LazyLogging
import combinations.CombinationBuilder

import scala.util.Random

class WorkloadRunner(dataset: Dataset, selectedXDimensions: Int, sampleSize: Int) extends LazyLogging {

  val randomSeed = 123
  val yzSample = WorkloadRunner.takeSample(dataset.data, sampleSize, randomSeed)

  def run(dimensions: Vector[Int], rows: Vector[Int]): Option[Result] = {

    // pull rows and reduce dimensions
    val xyzReduced = new Array[XYZ](rows.length)
    for (row <- rows.indices) {
      val oldRow = dataset.data(rows(row))
      val newRow = XYZ(oldRow.id, oldRow.x, subsample(oldRow.y, dimensions), oldRow.z)
      xyzReduced(row) = newRow
    }

    val (points, coeff1, coeff2) = WorkloadRunner.constructRednessScores(xyzReduced, dataset.data, dimensions)

    val dnfToIndices = CombinationBuilder(dataset.xLength, selectedXDimensions).all().flatMap { indices =>
      val (a,b) = (indices(0)+2,indices(1)+2)
      val combinations = Vector((a, b), (-a, b), (a, -b), (-a, -b))
      val result = combinations.map { case (i1,i2) =>
        // Search for the set of Points that fits this particular index pair
        val filteredPoints = selectBooleanValuesAtIndices(points, Vector((Math.abs(i1), i1 > 0), (Math.abs(i2), i2 > 0)))
        (filteredPoints, (i1,i2))
      }
      result
    }.toMap
    val (kDNF, error) = new SetCover(dnfToIndices.keySet, 0.2, dataset.data.length).lowDegPartial2(true)
    val kDNFString = kDNF.map(dnfToIndices).toString

//    if (setCoverResult.error < 0.4) {
      Some(Result(dimensions, rows, Vector(coeff1, coeff2), error, kDNFString))
//    } else {
//      None
//    }
  }
}

object WorkloadRunner {

  private def takeSample(data: Array[XYZ], sampleSize: Int, seed: Int): Array[XYZ] = {
    val r = new Random(seed)
    r.shuffle(data).take(sampleSize)
  }

  private def subsample(arr: Array[Double], indices: Vector[Int]): Array[Double] = {
    val result = new Array[Double](indices.length)
    for (i <- indices.indices) {
      result(i) = arr(indices(i))
    }
    result
  }

  private def constructRednessScores(xyzReduced: Array[XYZ], xyzOriginal: Array[XYZ], dimensions: Vector[Int]) = {
    val xyz1 = xyzReduced(0)
    val xyz2 = xyzReduced(1)
    val x1 = xyz1.y(0)
    val y1 = xyz1.y(2)
    val z1 = xyz1.z

    val x2 = xyz2.y(0)
    val y2 = xyz2.y(2)
    val z2 = xyz2.z

    val a1 = (z1*y2- z2*y1) / (x1*y2-x2*y1)
    val a2 = (x1*z2- x2*z1) / (x1*y2-x2*y1)

    val points = xyzOriginal.map { xyz =>
      val redness = Math.abs(xyz.z - a1*xyz.y(dimensions(0)) - a2*xyz.y(dimensions(1)))
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