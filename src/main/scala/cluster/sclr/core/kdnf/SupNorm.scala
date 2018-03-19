package cluster.sclr.core.kdnf

import Jama.Matrix
import cluster.sclr.Messages.Workload
import cluster.sclr.core.{Dataset, Result, XYZ}
import combinations.CombinationBuilder

class SupNorm(workload: Workload) extends KdnfStrategy {
  val dnfSize = workload.dnfSize
  val mu = workload.mu
  val epsilon = workload.optionalEpsilon.get

  def run(dataset: Dataset, yDimensions: Vector[Int], rows: Vector[Int]): Option[Result] = {
    var kdnf = ""
    var error = 0.0
    var coeff = Vector(0.0)
    val M = dataset.data.length
    val ro = List(-1, 1)
    for (r1 <- ro; r2 <- ro; r3 <- ro) {
      val (coeff1, coeff2, epsilon2) = solveLinearSystem(dataset.data, yDimensions, rows, r1, r2, r3)

      if (epsilon2 <= epsilon && epsilon2 > 0) {
        var allDNFTerms = CombinationBuilder(dataset.xLength, dnfSize).all().flatMap { zeroIndexedIndices =>
          val (a, b) = (zeroIndexedIndices(0) + 1, zeroIndexedIndices(1) + 1)
          val combinations = Vector((a, b), (-a, b), (a, -b), (-a, -b))
          combinations
        }.toList

        for (i <- 0 until M) {
          val xyz = dataset.data(i)
          val y1 = xyz.y(yDimensions(0))
          val y2 = xyz.y(yDimensions(1))

          if (Math.abs(coeff1 * y1 + coeff2 * y2 - xyz.z) > epsilon) {
            allDNFTerms = allDNFTerms.filterNot(p =>
              ((xyz.x(Math.abs(p._1) - 1) && p._1 > 0) || (!xyz.x(Math.abs(p._1) - 1) && p._1 < 0)) &&
                ((xyz.x(Math.abs(p._2) - 1) && p._2 > 0) || (!xyz.x(Math.abs(p._2) - 1) && p._2 < 0))
            )
          }

        }

        if (allDNFTerms.nonEmpty) {
          val points = dataset.data.filter(xyz =>
            allDNFTerms.exists(p =>
              ((xyz.x(Math.abs(p._1) - 1) && p._1 > 0) || (!xyz.x(Math.abs(p._1) - 1) && p._1 < 0)) &&
                ((xyz.x(Math.abs(p._2) - 1) && p._2 > 0) || (!xyz.x(Math.abs(p._2) - 1) && p._2 < 0))
            )
          )

          if (points.length > mu * M) {
            kdnf = allDNFTerms.toString()
            error = epsilon2
            coeff = Vector(coeff1, points.length)
          }
        }
      }
    }
    if (kdnf.length > 0)
      Some(Result(yDimensions, rows, coeff, error, kdnf))
    else
      None
  }

  private def solveLinearSystem(data: Array[XYZ], yDimensions: Vector[Int], rows: Vector[Int], r1: Int, r2: Int, r3: Int) = {
    val xyz1 = data(rows(0))
    val xyz2 = data(rows(1))
    val xyz3 = data(rows(2))

    val A = new Matrix(3, 3)
    A.set(0, 0, xyz1.y(yDimensions(0)))
    A.set(0, 1, xyz1.y(yDimensions(1)))
    A.set(0, 2, -1 * r1)
    A.set(1, 0, xyz2.y(yDimensions(0)))
    A.set(1, 1, xyz2.y(yDimensions(1)))
    A.set(1, 2, -1 * r2)
    A.set(2, 0, xyz3.y(yDimensions(0)))
    A.set(2, 1, xyz3.y(yDimensions(1)))
    A.set(2, 2, -1 * r3)
    val b = new Matrix(3, 1)
    b.set(0, 0, xyz1.z)
    b.set(1, 0, xyz2.z)
    b.set(2, 0, xyz3.z)

    val coeff = A.solve(b)
    (coeff.get(0, 0), coeff.get(1, 0), coeff.get(2, 0))
  }
}
