package sclr.core.strategy

import scala.collection.immutable.BitSet
import scala.collection.mutable

/**
  * Consider only building the kDNF representation and comparing it to the data directly to get set size.
  */
class SetCover(allTerms: Vector[BitSet], mu: Double, beta: Int, simpleAlgorithm: Boolean) {

  def lowDegPartial2(idToRedness: Map[Int, Double]): (Vector[BitSet], Double) = {

    def calculateTermCostsMap(terms: Vector[BitSet]): Map[BitSet, Double] = {
      terms.map { term =>
        val termTotalCost = term.foldLeft(0.0)((accumulator, term) => accumulator + idToRedness(term))
        val termAverageCost = termTotalCost / term.size.toDouble
        term -> termAverageCost
      }.toMap
    }

    def degree(r_i: Int, terms: Vector[BitSet]): Double = {
      terms.count(_.contains(r_i)) * idToRedness(r_i)
    }

    val rednessCache = new mutable.HashMap[BitSet, Double]()
    def rednessOfTerm(term: BitSet): Double = {
      rednessCache.getOrElseUpdate(term, term.foldLeft(0.0)((accum, p) => accum + idToRedness(p)))
    }

    def rednessOfTerms(terms: Vector[BitSet]): Double = {
      terms.foldLeft(0.0)((accum, term) => accum + rednessOfTerm(term))
    }

    def errorRate(kDNF: Vector[BitSet]): Double = {
      val error = rednessOfTerms(kDNF) / union(kDNF).size
      if (error.isNaN) Double.MaxValue else error
    }

    lazy val costsCache = calculateTermCostsMap(allTerms)
    lazy val sortedCostsCache = costsCache.toSeq.sortBy(_._2)
    def simpleGreedy(idToRedness: Map[Int, Double], idCount: Int, totalRedness: Double)(rednessThreshold: Double): Vector[BitSet] = {
      val sortedCosts = new mutable.ListBuffer[BitSet]()
      var i = 0
      while (mu * idCount - union(sortedCosts.toVector).size > 0) {
        sortedCosts.append(sortedCostsCache(i)._1)
        i += 1
      }
      sortedCosts.toVector
    }

    def complexGreedy(idToRedness: Map[Int, Double], idCount: Int, totalRedness: Double)(rednessThreshold: Double): Vector[BitSet] = {
      val survivors = allTerms.filter(set => rednessOfTerm(set) <= rednessThreshold)
      if (union(survivors).size < mu * idCount) {
        return Vector[BitSet]()
      }

      val y = Math.sqrt(idCount / harmonic(Math.floor(mu * beta).toInt))
      val highDegreePoints = union(survivors).filter(p => degree(p, survivors) > y)
      val cleanedDnfToOriginalMap = mapCleanedDnfsToOriginals(survivors, highDegreePoints)
      val cleanedDnf = cleanedDnfToOriginalMap.keys.toVector
      partialGreedy(idCount, cleanedDnf).map(cleanedDnfToOriginalMap)
    }

    def partialGreedy(idCount: Int, cleanedDnfs: Vector[BitSet]): Vector[BitSet] = {
      val dnfCosts = calculateTermCostsMap(cleanedDnfs)

      val original = new mutable.ListBuffer[BitSet]
      for (t <- cleanedDnfs) {
        original.append(t)
      }
      val result = new mutable.ListBuffer[BitSet]
      while (mu * idCount - union(result.toVector).size > 0 && original.nonEmpty) {
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


    val idCount = idToRedness.keys.size
    val totalRedness = idToRedness.values.sum
    var minError = Double.MaxValue
    var bestKDNF = Vector[BitSet]()
    var rednessThreshold = Math.max(0.01, idToRedness.values.min)

    val alg: Double => Vector[BitSet] =
      if (simpleAlgorithm)
        simpleGreedy(idToRedness, idCount, totalRedness)
      else
        complexGreedy(idToRedness, idCount, totalRedness)
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

  private def union(sets: Vector[BitSet]): BitSet = {
    if (sets.isEmpty) BitSet.empty else sets.reduceLeft((accum, b) => accum.union(b))
  }

  // IMPORTANT NOTE!!!
  // For some reason, when we use this cache, it seems the system occasionally gets into an infinite loop. I don't
  // understand why, but it happens.
//  private val unionCache = new mutable.HashMap[Vector[BitSet], BitSet]()
//  private def union(sets: Vector[BitSet]): BitSet = {
//    unionCache.getOrElseUpdate(sets, if (sets.isEmpty) BitSet.empty else sets.reduceLeft((accum, b) => accum.union(b)))
//  }

  private def harmonic(n: Int): Double = {
    (1 to n).foldLeft(0.0)((a,b) => a + 1.0 / b)
  }

  private def mapCleanedDnfsToOriginals(terms: Vector[BitSet], r: BitSet): Map[BitSet, BitSet] = {
    terms.map { term => (term -- r) -> term}.toMap
  }

}
