package cluster.sclr.core

import scala.collection.immutable.BitSet
import scala.collection.mutable

/**
  * Consider only building the kDNF representation and comparing it to the data directly to get set size.
  */
class SetCover(allDnfs: Vector[BitSet], idToRedness: Map[Int, Double], mu: Double, beta: Int) {

  lazy val totalPointCount = idToRedness.keys.max
  lazy val lowestRedness = idToRedness.values.min
  lazy val totalRedness = idToRedness.values.sum

  def lowDegPartial2(simpleAlgorithm: Boolean): (Vector[BitSet], Double) = {
    var minError = Double.MaxValue
    var bestKDNF = Vector[BitSet]()
    var rednessThreshold = Math.max(0.01, lowestRedness)

    val alg: (Double) => Vector[BitSet] = if (simpleAlgorithm) simpleGreedy else complexGreedy
    while (rednessThreshold < totalRedness) {
      val kDNF = alg(rednessThreshold)
      val newError = errorRate(kDNF)
      if (minError > newError) {
        minError = newError
        bestKDNF = kDNF
      }
      rednessThreshold *= 1.1
    }
    (bestKDNF, minError)
  }

  private lazy val costsCache = calculateDnfCostsMap(allDnfs)
  private lazy val sortedCostsCache = costsCache.toSeq.sortBy(_._2)
  def simpleGreedy(rednessThreshold: Double): Vector[BitSet] = {
    val sortedCosts = new mutable.ListBuffer[BitSet]()
    var i = 0
    while (mu * totalPointCount - unionSize(sortedCosts.toVector) > 0) {
      sortedCosts.append(sortedCostsCache(i)._1)
      i += 1
    }
    sortedCosts.toVector
  }

  def complexGreedy(rednessThreshold: Double): Vector[BitSet] = {
    val survivors = allDnfs.filter(set => rednessOfSet(set) <= rednessThreshold)
    if (unionSize(survivors) < mu * totalPointCount) {
      return Vector[BitSet]()
    }

    val y = Math.sqrt(totalPointCount / harmonic(Math.floor(mu * beta).toInt))
    val highDegreePoints = union(survivors).filter(p => degree(p, survivors) > y)
    val cleanedDnfToOriginalMap = mapCleanedDnfsToOriginals(survivors, highDegreePoints)
    val cleanedDnf = cleanedDnfToOriginalMap.keys.toVector
    partialGreedy(cleanedDnf).map(cleanedDnfToOriginalMap)
  }

  private def partialGreedy(cleanedDnfs: Vector[BitSet]): Vector[BitSet] = {
    val dnfCosts = calculateDnfCostsMap(cleanedDnfs)

    val original = new mutable.ListBuffer[BitSet]
    for (t <- cleanedDnfs) {
      original.append(t)
    }
    val result = new mutable.ListBuffer[BitSet]
    while (mu * totalPointCount - unionSize(result.toVector) > 0 && original.nonEmpty) {
      var tMin = original.head
      for (t <- original) {
        if (dnfCosts(t) < dnfCosts(tMin)) {
          tMin = t
        }
      }
      result += tMin
      original -= tMin
    }
    result.toVector
  }

  def errorRate(kDNF: Vector[BitSet]): Double = {
    val error = rednessOfSets(kDNF) / unionSize(kDNF)
    if (error.isNaN) Double.MaxValue else error
  }

  private def calculateDnfCostsMap(dnfs: Vector[BitSet]): Map[BitSet, Double] = {
    dnfs.map { dnf =>
      val dnfTotalCost = dnf.foldLeft(0.0)((accum, p) => accum + idToRedness(p))
      dnf -> (dnfTotalCost / dnf.size.toDouble)
    }.toMap
  }

  private def union(sets: Vector[BitSet]): BitSet = {
    sets.reduceLeft((accum, b) => accum.union(b))
  }

  private def unionSize(sets: Vector[BitSet]): Int = {
    if (sets.isEmpty) {
      0
    } else {
      union(sets).size
    }
  }

  private def harmonic(n: Int): Double = {
    (1 to n).foldLeft(0.0)((a,b) => a + 1.0 / b)
  }

  private def mapCleanedDnfsToOriginals(sets: Vector[BitSet], r: BitSet): Map[BitSet, BitSet] = {
    sets.map { s => (s -- r) -> s}.toMap
  }

  private def degree(r_i: Int, sets: Vector[BitSet]): Double = {
    sets.count(_.contains(r_i)) * idToRedness(r_i)
  }

  private def rednessOfSets(sets: Vector[BitSet]): Double = {
    sets.foldLeft(0.0)((accum, set) => accum + rednessOfSet(set))
  }

  private val rednessCache = new mutable.HashMap[BitSet, Double]()
  private def rednessOfSet(dnf: BitSet): Double = {
    rednessCache.getOrElseUpdate(dnf, dnf.foldLeft(0.0)((accum, p) => accum + idToRedness(p)))
  }

}
