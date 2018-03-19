package cluster.sclr.core

import cluster.sclr.core.WorkloadRunner._
import com.typesafe.scalalogging.LazyLogging
import combinations.CombinationBuilder
import scala.util.control.Breaks._
class WorkloadRunner(dataset: Dataset) extends LazyLogging {

  def runSupNorm(dnfSize: Int, mu: Double, yDimensions: Vector[Int], rows: Vector[Int]): Option[Result] = {
        var kdnf = ""
        var error = 0.0
        var coeff = Vector(0.0)
        val M = dataset.data.length
        val epsilon = 0.45
        val ro = List(-1,1)
        for (r1 <- ro){
          for (r2 <- ro){
            for (r3 <- ro){
              val (coeff1, coeff2, epsilon2) = WorkloadRunner.solveLinearSystem(dataset.data,yDimensions,rows, r1, r2, r3)

              if (epsilon2 <= epsilon && epsilon2>0){
                var allDNFTerms = CombinationBuilder(dataset.xLength, dnfSize).all().flatMap { zeroIndexedIndices =>
                  val (a, b) = (zeroIndexedIndices(0) + 1, zeroIndexedIndices(1) + 1)
                  val combinations = Vector((a, b), (-a, b), (a, -b), (-a, -b))
                  combinations
                }.toList

                for (i <- 0 until M){
                  val xyz = dataset.data(i)
                  val y1 = xyz.y(yDimensions(0))
                  val y2 = xyz.y(yDimensions(1))

                  if (Math.abs(coeff1*y1+coeff2*y2 -xyz.z) > epsilon){
                    allDNFTerms = allDNFTerms.filterNot(p =>
                      ((xyz.x(Math.abs(p._1)-1) && p._1>0) || (!xyz.x(Math.abs(p._1)-1) && p._1 <0)) &&
                        ((xyz.x(Math.abs(p._2)-1) && p._2>0) || (!xyz.x(Math.abs(p._2)-1) && p._2 <0))
                    )
                  }

                }

                if (allDNFTerms.nonEmpty){
                  val points = dataset.data.filter(xyz =>
                    allDNFTerms.exists(p=>
                      ((xyz.x(Math.abs(p._1)-1) && p._1>0) || (!xyz.x(Math.abs(p._1)-1) && p._1 <0)) &&
                        ((xyz.x(Math.abs(p._2)-1) && p._2>0) || (!xyz.x(Math.abs(p._2)-1) && p._2 <0))
                    )
                  )

                  if (points.length > 0.24*M){
                    kdnf = allDNFTerms.toString()
                    error = epsilon2
                    coeff = Vector(coeff1, points.length)
                  }
                }
              }
            }
          }
        }
        if (kdnf.length > 0)
          Some(Result(yDimensions, rows, coeff, error, kdnf))
        else
          None
  }

  def runL2Norm(dnfSize: Int, mu: Double, yDimensions: Vector[Int], rows: Vector[Int]): Option[Result] = {
    val (points, coeff1, coeff2) = WorkloadRunner.constructRednessScores(dataset.data, yDimensions, rows)

    val dnfToIndices = CombinationBuilder(dataset.xLength, dnfSize).all().flatMap { zeroIndexedIndices =>
      // Our indices are 0-indexed, so increase that to 1-indexed to allow negative indices.
      //      val oneIndexedIndices = zeroIndexedIndices.map(_+1)
      val (a, b) = (zeroIndexedIndices(0) + 1, zeroIndexedIndices(1) + 1)
      val combinations = Vector((a, b), (-a, b), (a, -b), (-a, -b))
      val result = combinations.map { case (i1, i2) =>
        // Search for the set of Points that fits this particular index pair
        val filteredPoints = selectBooleanValuesAtIndices(points, Vector((Math.abs(i1) - 1, i1 > 0), (Math.abs(i2) - 1, i2 > 0)))
        (filteredPoints, (i1, i2))
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

  def run(dnfSize: Int, mu: Double, yDimensions: Vector[Int], rows: Vector[Int]): Option[Result] = {
    runSupNorm(dnfSize, mu, yDimensions, rows)
  }
}

object WorkloadRunner {

  import Jama._

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

    val points = data.map { xyz =>
      val redness = Math.pow(xyz.z - a1 * xyz.y(yDimensions(0)) - a2 * xyz.y(yDimensions(1)), 2)
      Point(xyz, redness)
    }
    (points, a1, a2)
  }

  private def find2DNFsByRemovingMatches(points: Array[Point], terms: Vector[((Int, Boolean), (Int, Boolean))]) = {
    // Problem! This iterates over the same points over and over for each term! Inefficient.
    terms.filter { term =>
      points.exists { point =>
        point.xyz.x(term._1._1) == term._1._2 &&
          point.xyz.x(term._2._1) == term._2._2
      }
    }
  }

  private def removeMatchesForPoints(points: Array[Point], terms: Vector[((Int, Boolean), (Int, Boolean))]) = {
    var remainingTerms = terms
    val pointIterator = points.iterator
    while (remainingTerms.nonEmpty && pointIterator.hasNext) {
      val point = pointIterator.next()
      remainingTerms = removeMatchesForPoint(point, remainingTerms)
    }
  }

  private def removeMatchesForPoint(point: Point, terms: Vector[((Int, Boolean), (Int, Boolean))]): Vector[((Int, Boolean), (Int, Boolean))] = {
    ???
  }

  private def selectBooleanValuesAtIndices(points: Array[Point], selections: Vector[(Int, Boolean)]) = {
    points.filter { point =>
      selections.map(selection => point.xyz.x(selection._1) == selection._2).forall(identity)
    }.toSet
  }

}