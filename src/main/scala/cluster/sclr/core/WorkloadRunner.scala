package cluster.sclr.core

import com.typesafe.scalalogging.LazyLogging
import combinations.CombinationBuilder
import weka.core.{Attribute, DenseInstance, Instance, Instances, SelectedTag}
import weka.filters.Filter
import weka.filters.unsupervised.attribute.{Add, Remove}
import weka.filters.unsupervised.instance.{RemoveWithValues, Resample, SubsetByExpression}

import scala.collection.mutable.ListBuffer

class WorkloadRunner(x: Instances, yz: Instances, xDimensionSelected: Int, yzSampleSize: Int) extends LazyLogging {

  val randomSeed = 123
  val yzSample = WorkloadRunner.sampleInstances(yz, yzSampleSize, randomSeed)

  def run(dimensions: Vector[Int], rows: Vector[Int]): Option[Result] = {
    // Add the last dimension (the Y values)
    val fullDimensions = dimensions :+ yz.numAttributes() - 1

    var yzReduced = new Instances(yzSample, 0)
    for (i <- rows.indices) {
      yzReduced.add(yzSample.get(rows(i)))
    }

    val attributeFilter = new Remove()
    attributeFilter.setInvertSelection(true)
    attributeFilter.setAttributeIndicesArray(fullDimensions.toArray)
    attributeFilter.setInputFormat(yzReduced)
    yzReduced = Filter.useFilter(yzReduced, attributeFilter)
    yzReduced.setClassIndex(yzReduced.numAttributes() - 1)
//    val model = new LinearRegression()
//    model.buildClassifier(yzReduced)
//    val weights = model.coefficients.toVector

    val (redness, coeff1, coeff2) = WorkloadRunner.constructRednessScores(attributeFilter, yzReduced, yz)

    val addAttribute = new Add()
    addAttribute.setAttributeName("redness")
    addAttribute.setAttributeType(new SelectedTag(Attribute.NUMERIC, Add.TAGS_TYPE))
    addAttribute.setAttributeIndex("last")
    addAttribute.setInputFormat(x)
    val xWithRedness = Filter.useFilter(x, addAttribute)
    for (i <- redness.indices) {
      xWithRedness.instance(i).setValue(xWithRedness.numAttributes()-1, redness(i))
    }
    val setMap = WorkloadRunner.constructSetsFromLabeledInstances(x.numAttributes() - 1, xDimensionSelected, xWithRedness)
    val setCoverResult = SetCover.lowDegPartial2(setMap.keySet, 0.2, x.size())
    val kDNF = setCoverResult.kDNF.map(setMap).toString

//    if (setCoverResult.error < 0.4) {
      Some(Result(dimensions, rows, Vector(coeff1, coeff2), setCoverResult.error, kDNF))
//    } else {
//      None
//    }
  }
}

object WorkloadRunner {

  private def sampleInstances(instances: Instances, sampleSize: Int, randomSeed: Int): Instances = {
    val filter = new Resample()
    val sampleSizePercent = (sampleSize.toDouble / instances.size().toDouble) * 100.0 // should be between 0 and 100
    filter.setInputFormat(instances)
    filter.setSampleSizePercent(sampleSizePercent)
    filter.setNoReplacement(true)
    filter.setRandomSeed(randomSeed)
    Filter.useFilter(instances, filter)
  }

  private def constructRednessScores(filter: Remove, yzReduced: Instances, yz: Instances) = {
    val x1 = yzReduced.get(0).value(0)
    val y1 = yzReduced.get(0).value(1)
    val z1 = yzReduced.get(0).value(2)

    val x2 = yzReduced.get(1).value(0)
    val y2 = yzReduced.get(1).value(1)
    val z2 = yzReduced.get(1).value(2)

    val a1 = (z1*y2- z2*y1) / (x1*y2-x2*y1)
    val a2 = (x1*z2- x2*z1) / (x1*y2-x2*y1)

    val point1 = new DenseInstance(3)
    point1.setValue(0, x1)
    point1.setValue(1, y1)
    point1.setValue(2, z1)

    val point2 = new DenseInstance(3)
    point2.setValue(0, x2)
    point2.setValue(1, y2)
    point2.setValue(2, z2)

    filter.setInputFormat(yz)
    val redness = new ListBuffer[Double]
    val yzFilter = Filter.useFilter(yz, filter)

    for (i <- 0 until yz.size) {
      redness.append(Math.abs(yzFilter.get(i).value(2) - a1*yzFilter.get(i).value(0) - a2*yzFilter.get(i).value(1)))
    }
    (redness.toVector, a1, a2)
  }

  private def constructSetsFromLabeledInstances(xDimensionCount: Int, xDimensionSelected: Int, x: Instances): Map[Set[Instance], (Int, Int)] = {

    val indexCombinations = CombinationBuilder(xDimensionCount, xDimensionSelected).all().flatMap { c =>
        val (a,b) = (c(0)+2,c(1)+2)
        Vector(Vector(a, b), Vector(-a, b), Vector(a, -b), Vector(-a, -b))
    }

    val allSets = indexCombinations.map { indices =>
      val (a,b) = (indices(0), indices(1))
      // Search x for the set of Instances that fits this particular index pair
      val xFiltered = selectBooleanValuesAtIndex(selectBooleanValuesAtIndex(x, Math.abs(a), a > 0), Math.abs(b), b > 0)
      import collection.JavaConverters._
      (xFiltered.asScala.toSet, (a,b))
    }.toMap
    allSets
  }

  private def selectBooleanValuesAtIndex(instances: Instances, index: Int, isTrue: Boolean) = {
    val f = new RemoveWithValues()
    f.setSplitPoint(0.5)
    f.setInvertSelection(!isTrue)
    f.setAttributeIndex(index.toString)
    f.setInputFormat(instances)
    Filter.useFilter(instances, f)
  }

}