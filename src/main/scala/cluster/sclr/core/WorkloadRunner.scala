package cluster.sclr.core

import cluster.sclr.Messages.Workload
import weka.classifiers.functions.LinearRegression
import weka.core.Instances
import weka.core.converters.ConverterUtils
import weka.filters.Filter
import weka.filters.unsupervised.attribute.Remove

class WorkloadRunner(val workload: Workload) {

  val instances: Instances = new ConverterUtils.DataSource(workload.dataset).getDataSet
  instances.setClassIndex(instances.numAttributes - 1)
  var model: LinearRegression = _

  /*
   * perform linear regression using those data & attributes specified in dimensions and rows
   */ @throws[Exception]
  def run(dimensions: Vector[Int], rows: Vector[Int]): Option[Result] = {
    // Add the last dimension (the Y values)
    dimensions :+ instances.numAttributes() - 1

    var reducedInst = new Instances(instances, 0)
    for (i <- rows.indices) {
      reducedInst.add(instances.get(rows(i)))
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
    Some(Result(dimensions, rows, model.coefficients.toVector, 0.0, "kDNF?"))
  }

  private def redBluePartialCoverSet() = {

  }


}
