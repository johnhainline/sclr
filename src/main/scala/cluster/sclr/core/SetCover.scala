package cluster.sclr.core

import weka.core.Instance

import scala.collection.mutable


case class SetCoverResult(kDNF: Set[Set[Instance]], rednessThreshold: Double, error: Double)

object SetCover {

  def redness(instance: Instance): Double = {
    instance.value(instance.numAttributes()-1)
  }

  def union(sets: Set[Set[Instance]]): Set[Instance] = {
    sets.reduceLeft((a, b) => a.union(b))
  }

  def unionSize(sets: Set[Set[Instance]]): Int = {
    if (sets.isEmpty) {
      0
    } else {
      union(sets).size
    }
  }

  def degree(r_i: Instance, sets: Set[Set[Instance]]): Double = {
    sets.count(_.contains(r_i)) * redness(r_i)
  }

  def harmonic(n: Int): Double = {
    (1 to n).foldLeft(0.0)((a,b) => a + 1.0 / b)
  }

  def phi(set: Set[Instance], r: Set[Instance]): Set[Instance] = {
    set -- r
  }

  def phi2(sets: Set[Set[Instance]], r: Set[Instance]): Map[Set[Instance], Set[Instance]] = {
    sets.map { s => phi(s, r) -> s}.toMap
  }

  def phi2(sets: Set[Set[Instance]]): Map[Set[Instance], Set[Instance]] = {
    sets.map {s => s -> s}.toMap
  }

  def addRednessCollection(sets: Set[Set[Instance]]): Double = {
    var total = 0.0
    for (point <- union(sets)) {
      total += redness(point)
    }
    total
  }

  def addRedness(set: Set[Instance]): Double = {
    var total = 0.0
    for (point <- set) {
      total += redness(point)
    }
    total
  }

  /*
   * Algorithm 1
   */
  def partialGreedy(list: Set[Set[Instance]], costs: Map[Set[Instance], Double], mu: Double, totalInstanceCount: Int): Set[Set[Instance]] = {
    val original = new mutable.HashSet[Set[Instance]]
    for (t <- list) {
      original.add(t)
    }
    val sol = new mutable.HashSet[Set[Instance]]
    while (mu * totalInstanceCount - unionSize(sol.toSet) > 0 && original.nonEmpty) {
      var tMin = original.head
      for (t <- original) {
        if (costs(t) / t.size < costs(tMin) / tMin.size) {
          tMin = t
        }
      }
      sol.add(tMin)
      original.remove(tMin)
    }
    sol.toSet
  }

  /*
   * Algorithm 2
   */
  def greedyPartialRB(totalInstanceCount: Int, list: Set[Set[Instance]], mu: Double): Set[Set[Instance]] = {
    val transform = phi2(list)
    val costs = new mutable.HashMap[Set[Instance], Double]()
    val convert = new mutable.HashSet[Set[Instance]]()
    for (set <- transform.keySet) {
      convert.add(set)
      costs.put(set, addRedness(transform(set)))
    }
    val resultFromPG = partialGreedy(convert.toSet, costs.toMap, mu, totalInstanceCount)
    val result = new mutable.HashSet[Set[Instance]]
    for (t <- resultFromPG) {
      result.add(transform(t))
    }
    result.toSet
  }

  /*
   * Low Deg Partial(X)
   */
  def lowDegPartial(totalInstanceCount: Int, sets: Set[Set[Instance]], mu: Double, rednessThreshold: Double, beta: Int): Set[Set[Instance]] = {
    val survivors = sets.filter(set => addRedness(set) <= rednessThreshold)
    if (unionSize(survivors) < mu * totalInstanceCount) {
      return Set.empty
    }

    val Y = Math.sqrt(sets.size / harmonic(Math.floor(mu * beta).toInt))
    val highDegreeInstances = union(survivors).filter(Instance => degree(Instance, survivors) > Y)
    val lowDegreeInstances = phi2(survivors, highDegreeInstances)
    val SXY = new mutable.HashSet[Set[Instance]]
    for (s <- lowDegreeInstances.keySet) {
      SXY.add(s)
    }
    val resultFromAlgo2 = greedyPartialRB(totalInstanceCount, SXY.toSet, mu)
    val finalResult = new mutable.HashSet[Set[Instance]]
    for (s <- resultFromAlgo2) {
      finalResult.add(lowDegreeInstances(s))
    }
    finalResult.toSet
  }

  def errorRate(totalInstanceCount: Int, sets: Set[Set[Instance]]): Double = {
    val error = addRednessCollection(sets) / unionSize(sets)
    if (error.isNaN) 1.0 else error
  }

  def lowDegPartial2(setOfSets: Set[Set[Instance]], mu: Double, beta: Int): SetCoverResult = {
    val allInstances = union(setOfSets)
    val totalInstanceCount = allInstances.size
    val totalRedness = addRedness(allInstances)
    var rednessThreshold = redness(allInstances.minBy(redness))
    var kDNF = lowDegPartial(totalInstanceCount, setOfSets, mu, rednessThreshold, beta)
    var minError = errorRate(totalInstanceCount, setOfSets)
    var i = rednessThreshold
    while (i < totalRedness) {
      kDNF = lowDegPartial(totalInstanceCount, setOfSets, mu, i, beta)
      val ithError = errorRate(totalInstanceCount, setOfSets)
      if (minError > ithError) {
        rednessThreshold = i
        minError = ithError
      }
      i = i * 1.1
    }
    SetCoverResult(kDNF, rednessThreshold, minError)
  }
}
