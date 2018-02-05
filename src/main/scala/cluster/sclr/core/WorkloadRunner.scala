package cluster.sclr.core

import cluster.sclr.Messages.Workload
import combinations.CombinationBuilder
import weka.classifiers.functions.LinearRegression
import weka.core.Instances
import weka.core.converters.ConverterUtils
import weka.filters.Filter
import weka.filters.unsupervised.attribute.Remove
import weka.filters.unsupervised.instance.SubsetByExpression

class WorkloadRunner(val workload: Workload) {

  val x  = new ConverterUtils.DataSource(workload.dataX).getDataSet
  val yz = new ConverterUtils.DataSource(workload.dataYZ).getDataSet
  yz.setClassIndex(yz.numAttributes - 1)

  /*
   * perform linear regression using those data & attributes specified in dimensions and rows
   */ @throws[Exception]
  def run(dimensions: Vector[Int], rows: Vector[Int]): Option[Result] = {
    // Add the last dimension (the Y values)
    dimensions :+ yz.numAttributes() - 1

    var reducedInst = new Instances(yz, 0)
    for (i <- rows.indices) {
      reducedInst.add(yz.get(rows(i)))
    }

    val attributeFilter = new Remove()
    attributeFilter.setInvertSelection(true)
    attributeFilter.setAttributeIndicesArray(dimensions.toArray)
    attributeFilter.setInputFormat(reducedInst)
    reducedInst = Filter.useFilter(reducedInst, attributeFilter)
    reducedInst.setClassIndex(reducedInst.numAttributes() - 1)
    System.out.println(reducedInst)
    val model = new LinearRegression()
    model.buildClassifier(reducedInst)
    System.out.println(model)
    val weights = model.coefficients.toVector
    constructSetsFromLabeledInstances()
    Some(Result(dimensions, rows, weights, 0.0, "kDNF?"))
  }

  private def constructSetsFromLabeledInstances() = {
    val indexCombinations = CombinationBuilder(x.numAttributes(), 2).all().flatMap { c =>
      val a = c.head + 1
      val b = c.last + 1
      Vector((a,b), (-a, b), (a, -b), (-a, -b))
    }

    val sets = indexCombinations.map { case (attr1,attr2) =>
      val f = new SubsetByExpression()
      val attr1String = s"x${Math.abs(attr1)}"
      val attr2String = s"x${Math.abs(attr2)}"
      val attr1Expr = attr1String.concat(if (attr1 > 0) " > 0" else " <= 0")
      val attr2Expr = attr2String.concat(if (attr2 > 0) " > 0" else " <= 0")
      f.setExpression(s"$attr1Expr AND $attr2Expr")
      f.setInputFormat(x)
      val set = Filter.useFilter(x, f)
      set
    }.toVector
    sets
  }
}
