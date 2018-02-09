package cluster.sclr.core

import weka.core.Instance

import scala.collection.mutable



class SetCover(allDnfs: Set[Set[Instance]], mu: Double, beta: Int) {

  lazy val allInstances = union(allDnfs)
  lazy val totalInstanceCount = allInstances.size
  lazy val lowestRedness = redness(allInstances.minBy(redness))
  lazy val totalRedness = rednessOfSet(allInstances)

  //kDNF: Set[Set[Instance]], error: Double
  def lowDegPartial2(simpleAlgorithm: Boolean): (Set[Set[Instance]], Double) = {
    var minError = Double.MaxValue
    var bestKDNF = Set.empty[Set[Instance]]
    var rednessThreshold = lowestRedness

    val alg: (Double) => Set[Set[Instance]] = if (simpleAlgorithm) simpleGreedy else complexGreedy
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
  def simpleGreedy(rednessThreshold: Double): Set[Set[Instance]] = {
    val sortedCosts = new mutable.HashSet[Set[Instance]]()
    var i = 0
    while (mu * totalInstanceCount - unionSize(sortedCosts.toSet) > 0) {
      sortedCosts.add(sortedCostsCache(i)._1)
      i += 1
    }
    sortedCosts.toSet
  }

  def complexGreedy(rednessThreshold: Double): Set[Set[Instance]] = {
    val survivors = allDnfs.filter(set => rednessOfSet(set) <= rednessThreshold)
    if (unionSize(survivors) < mu * totalInstanceCount) {
      return Set.empty
    }

    val y = Math.sqrt(totalInstanceCount / harmonic(Math.floor(mu * beta).toInt))
    val highDegreeInstances = union(survivors).filter(Instance => degree(Instance, survivors) > y)
    val cleanedDnfToOriginalMap = mapCleanedDnfsToOriginals(survivors, highDegreeInstances)
    val cleanedDnf = cleanedDnfToOriginalMap.keySet
    partialGreedy(cleanedDnf).map(cleanedDnfToOriginalMap)
  }

  private def partialGreedy(cleanedDnfs: Set[Set[Instance]]): Set[Set[Instance]] = {
    val dnfCosts = calculateDnfCostsMap(cleanedDnfs)

    val original = new mutable.HashSet[Set[Instance]]
    for (t <- cleanedDnfs) {
      original.add(t)
    }
    val result = new mutable.HashSet[Set[Instance]]
    while (mu * totalInstanceCount - unionSize(result.toSet) > 0 && original.nonEmpty) {
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

  def errorRate(kDNF: Set[Set[Instance]]): Double = {
    val error = rednessOfSets(kDNF) / unionSize(kDNF)
    if (error.isNaN) Double.MaxValue else error
  }

  private def calculateDnfCostsMap(dnfs: Set[Set[Instance]]): Map[Set[Instance], Double] = {
    dnfs.map { dnf =>
      val dnfTotalCost = dnf.foldLeft(0.0)((accum, instance) => accum + redness(instance))
      dnf -> (dnfTotalCost / dnf.size.toDouble)
    }.toMap
  }

  private def union(sets: Set[Set[Instance]]): Set[Instance] = {
    sets.reduceLeft((accum, b) => accum.union(b))
  }

  private def unionSize(sets: Set[Set[Instance]]): Int = {
    if (sets.isEmpty) {
      0
    } else {
      union(sets).size
    }
  }

  private def harmonic(n: Int): Double = {
    (1 to n).foldLeft(0.0)((a,b) => a + 1.0 / b)
  }

  private def mapCleanedDnfsToOriginals(sets: Set[Set[Instance]], r: Set[Instance]): Map[Set[Instance], Set[Instance]] = {
    sets.map { s => (s -- r) -> s}.toMap
  }

  private def degree(r_i: Instance, sets: Set[Set[Instance]]): Double = {
    sets.count(_.contains(r_i)) * redness(r_i)
  }

  private def rednessOfSets(sets: Set[Set[Instance]]): Double = {
    sets.foldLeft(0.0)((accum, set) => accum + rednessOfSet(set))
  }

  private val rednessCache = new mutable.HashMap[Set[Instance], Double]()
  private def rednessOfSet(dnf: Set[Instance]): Double = {
    rednessCache.getOrElseUpdate(dnf, dnf.foldLeft(0.0)((accum, instance) => accum + redness(instance)))
  }

  private def redness(instance: Instance): Double = {
    instance.value(instance.numAttributes()-1)
  }

}
