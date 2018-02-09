package cluster.sclr.core

import scala.collection.mutable

case class Point(xyz: XYZ, redness: Double)

class SetCover(allDnfs: Set[Set[Point]], mu: Double, beta: Int) {

  lazy val allPoints = union(allDnfs)
  lazy val totalPointCount = allPoints.size
  lazy val lowestRedness = allPoints.minBy(_.redness).redness
  lazy val totalRedness = rednessOfSet(allPoints)

  //kDNF: Set[Set[Point]], error: Double
  def lowDegPartial2(simpleAlgorithm: Boolean): (Set[Set[Point]], Double) = {
    var minError = Double.MaxValue
    var bestKDNF = Set.empty[Set[Point]]
    var rednessThreshold = lowestRedness

    val alg: (Double) => Set[Set[Point]] = if (simpleAlgorithm) simpleGreedy else complexGreedy
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
  def simpleGreedy(rednessThreshold: Double): Set[Set[Point]] = {
    val sortedCosts = new mutable.HashSet[Set[Point]]()
    var i = 0
    while (mu * totalPointCount - unionSize(sortedCosts.toSet) > 0) {
      sortedCosts.add(sortedCostsCache(i)._1)
      i += 1
    }
    sortedCosts.toSet
  }

  def complexGreedy(rednessThreshold: Double): Set[Set[Point]] = {
    val survivors = allDnfs.filter(set => rednessOfSet(set) <= rednessThreshold)
    if (unionSize(survivors) < mu * totalPointCount) {
      return Set.empty
    }

    val y = Math.sqrt(totalPointCount / harmonic(Math.floor(mu * beta).toInt))
    val highDegreePoints = union(survivors).filter(p => degree(p, survivors) > y)
    val cleanedDnfToOriginalMap = mapCleanedDnfsToOriginals(survivors, highDegreePoints)
    val cleanedDnf = cleanedDnfToOriginalMap.keySet
    partialGreedy(cleanedDnf).map(cleanedDnfToOriginalMap)
  }

  private def partialGreedy(cleanedDnfs: Set[Set[Point]]): Set[Set[Point]] = {
    val dnfCosts = calculateDnfCostsMap(cleanedDnfs)

    val original = new mutable.HashSet[Set[Point]]
    for (t <- cleanedDnfs) {
      original.add(t)
    }
    val result = new mutable.HashSet[Set[Point]]
    while (mu * totalPointCount - unionSize(result.toSet) > 0 && original.nonEmpty) {
      var tMin = original.head
      for (t <- original) {
        if (dnfCosts(t) < dnfCosts(tMin)) {
          tMin = t
        }
      }
      result.add(tMin)
      original.remove(tMin)
    }
    result.toSet
  }

  def errorRate(kDNF: Set[Set[Point]]): Double = {
    val error = rednessOfSets(kDNF) / unionSize(kDNF)
    if (error.isNaN) Double.MaxValue else error
  }

  private def calculateDnfCostsMap(dnfs: Set[Set[Point]]): Map[Set[Point], Double] = {
    dnfs.map { dnf =>
      val dnfTotalCost = dnf.foldLeft(0.0)((accum, p) => accum + p.redness)
      dnf -> (dnfTotalCost / dnf.size.toDouble)
    }.toMap
  }

  private def union(sets: Set[Set[Point]]): Set[Point] = {
    sets.reduceLeft((accum, b) => accum.union(b))
  }

  private def unionSize(sets: Set[Set[Point]]): Int = {
    if (sets.isEmpty) {
      0
    } else {
      union(sets).size
    }
  }

  private def harmonic(n: Int): Double = {
    (1 to n).foldLeft(0.0)((a,b) => a + 1.0 / b)
  }

  private def mapCleanedDnfsToOriginals(sets: Set[Set[Point]], r: Set[Point]): Map[Set[Point], Set[Point]] = {
    sets.map { s => (s -- r) -> s}.toMap
  }

  private def degree(r_i: Point, sets: Set[Set[Point]]): Double = {
    sets.count(_.contains(r_i)) * r_i.redness
  }

  private def rednessOfSets(sets: Set[Set[Point]]): Double = {
    sets.foldLeft(0.0)((accum, set) => accum + rednessOfSet(set))
  }

  private val rednessCache = new mutable.HashMap[Set[Point], Double]()
  private def rednessOfSet(dnf: Set[Point]): Double = {
    rednessCache.getOrElseUpdate(dnf, dnf.foldLeft(0.0)((accum, p) => accum + p.redness))
  }

}
