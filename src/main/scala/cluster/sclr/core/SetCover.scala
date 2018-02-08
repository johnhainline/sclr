package cluster.sclr.core

import scala.collection.mutable


object SetCover {
  def union(sets: Set[Set[Point]]): Set[Point] = {
    sets.reduceLeft((a, b) => a.union(b))
  }

  def unionSize(sets: Set[Set[Point]]): Int = {
    if (sets.isEmpty) {
      0
    } else {
      union(sets).size
    }
  }

  def degree(r_i: Point, sets: Set[Set[Point]]): Double = {
    sets.count(_.contains(r_i)) * r_i.redness
  }

  def harmonic(n: Int): Double = {
    (1 to n).foldLeft(0.0)((a,b) => a + 1.0 / b)
  }

  def phi(set: Set[Point], r: Set[Point]): Set[Point] = {
    set -- r
  }

  def phi2(sets: Set[Set[Point]], r: Set[Point]): Map[Set[Point], Set[Point]] = {
    sets.map { s => phi(s, r) -> s}.toMap
  }

  def phi2(sets: Set[Set[Point]]): Map[Set[Point], Set[Point]] = {
    sets.map {s => s -> s}.toMap
  }

  def addRednessCollection(sets: Set[Set[Point]]): Double = {
    var total = 0.0
    for (point <- union(sets)) {
      total += point.redness
    }
    total
  }

  def addRedness(set: Set[Point]): Double = {
    var total = 0.0
    for (point <- set) {
      total += point.redness
    }
    total
  }

  /*
   * Algorithm 1
   */
  def partialGreedy(list: Set[Set[Point]], costs: Map[Set[Point], Double], mu: Double, totalPointCount: Int): Set[Set[Point]] = {
    val original = new mutable.HashSet[Set[Point]]
    for (t <- list) {
      original.add(t)
    }
    val sol = new mutable.HashSet[Set[Point]]
    while (mu * totalPointCount - unionSize(sol.toSet) > 0 && original.nonEmpty) {
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
  def greedyPartialRB(totalPointCount: Int, list: Set[Set[Point]], mu: Double): Set[Set[Point]] = {
    val transform = phi2(list)
    val costs = new mutable.HashMap[Set[Point], Double]()
    val convert = new mutable.HashSet[Set[Point]]()
    for (set <- transform.keySet) {
      convert.add(set)
      costs.put(set, addRedness(transform(set)))
    }
    val resultFromPG = partialGreedy(convert.toSet, costs.toMap, mu, totalPointCount)
    val result = new mutable.HashSet[Set[Point]]
    for (t <- resultFromPG) {
      result.add(transform(t))
    }
    result.toSet
  }

  /*
   * Low Deg Partial(X)
   */
  def lowDegPartial(totalPointCount: Int, sets: Set[Set[Point]], mu: Double, rednessThreshold: Double, beta: Int): Set[Set[Point]] = {
    val survivors = sets.filter(set => addRedness(set) <= rednessThreshold)
    if (unionSize(survivors) < mu * totalPointCount) {
      return Set.empty
    }

    val Y = Math.sqrt(sets.size / harmonic(Math.floor(mu * beta).toInt))
    val highDegreePoints = union(survivors).filter(point => degree(point, survivors) > Y)
    val lowDegreePoints = phi2(survivors, highDegreePoints)
    val SXY = new mutable.HashSet[Set[Point]]
    for (s <- lowDegreePoints.keySet) {
      SXY.add(s)
    }
    val resultFromAlgo2 = greedyPartialRB(totalPointCount, SXY.toSet, mu)
    val finalResult = new mutable.HashSet[Set[Point]]
    for (s <- resultFromAlgo2) {
      finalResult.add(lowDegreePoints(s))
    }
    finalResult.toSet
  }

  def errorRate(totalPointCount: Int, sets: Set[Set[Point]]): Double = {
    addRednessCollection(sets) / unionSize(sets)
  }

  def lowDegPartial2(setOfSets: Set[Set[Point]], mu: Double, beta: Int): Double = {
    val allPoints = union(setOfSets)
    val totalPointCount = allPoints.size
    val totalRedness = addRedness(allPoints)
    var ans = allPoints.minBy(_.redness).redness
    var minError = errorRate(totalPointCount, lowDegPartial(totalPointCount, setOfSets, mu, ans, beta))
    var i = ans
    while (i < totalRedness) {
      val ithError = errorRate(totalPointCount, lowDegPartial(totalPointCount, setOfSets, mu, i, beta))
      if (minError > ithError) {
        ans = i
        minError = ithError
      }
      i = i * 1.1
    }
    ans
  }
}
