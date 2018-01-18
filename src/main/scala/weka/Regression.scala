package weka

import weka.classifiers.functions.LinearRegression
import weka.core.converters.ConverterUtils
import weka.core.{Instance, Instances}
import weka.filters.Filter
import weka.filters.unsupervised.attribute.Remove

class Regression(dataPath: String) {
  val instances: Instances = new ConverterUtils.DataSource(dataPath).getDataSet
  instances.setClassIndex(instances.numAttributes - 1)
  var model: LinearRegression = _

  /*
   * get the coefficients
   */ @throws[Exception]
  def coefficients: Vector[Double] = { // build a Linear Regression Model based on the data
    model = new LinearRegression()
    model.buildClassifier(this.instances)
    //		System.out.println(model);
    model.coefficients.toVector
  }

  /*
   * predict a data entry
   */ @throws[Exception]
  def predict(entry: Instance): Double = model.classifyInstance(entry)

  /*
   * perform linear regression using those data & attributes specified in dimensions and rows
   */ @throws[Exception]
  def linearRegressionSubAttributes(dimensions: Vector[Int], rows: Vector[Int]): Vector[Double] = {
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
    model.coefficients.toVector
  }

}
