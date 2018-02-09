package cluster.main

import weka.core.{Attribute, DenseInstance, Instance, Instances}
import weka.filters.{Filter, MultiFilter}
import weka.filters.unsupervised.instance.{RemoveWithValues, SubsetByExpression}

object WekaExperiments {

  private def createInstance(values: Vector[Double]): Instance = {
    val instance = new DenseInstance(values.length)
    for (i <- values.indices) {
      instance.setValue(i, values(i))
    }
    instance
  }

  private def subsetByExpression(instances: Instances, expression: String) = {
      val f = new SubsetByExpression()
      f.setExpression(expression)
      f.setInputFormat(instances)
      Filter.useFilter(instances, f)
  }

  private def selectBooleanValuesAtIndex(instances: Instances, index: Int, isTrue: Boolean) = {
    val f = new RemoveWithValues()
    f.setSplitPoint(0.5)
    f.setInvertSelection(!isTrue)
    f.setAttributeIndex(index.toString)
    f.setInputFormat(instances)
    Filter.useFilter(instances, f)
  }

  private def multiFilter(filters: Vector[Filter]) = {
    val f = new MultiFilter()
    f.setFilters(filters.toArray)
    f.setInputFormat(filters.head.getCopyOfInputFormat)
    f
  }

  def main(args: Array[String]): Unit = {
    val arrayList = new java.util.ArrayList[Attribute](4)
    arrayList.add(new Attribute("id"))
    arrayList.add(new Attribute("x1"))
    arrayList.add(new Attribute("x2"))
    arrayList.add(new Attribute("x3"))
    val instances = new Instances("name", arrayList, 3)
    val p1 = createInstance(Vector(0.0, 1.0, 0.0, 1.0))
    val p2 = createInstance(Vector(1.0, 1.0, 1.0, 1.0))
    val p3 = createInstance(Vector(2.0, 0.0, 0.0, 1.0))
    instances.add(p1)
    instances.add(p2)
    instances.add(p3)

    val f1 = selectBooleanValuesAtIndex(instances, 3, isTrue = false)
    val f2 = selectBooleanValuesAtIndex(f1, 2, isTrue = true)
    f2
//    val x1Filter = subsetByExpression(instances, "ATT2 > 0.0 and ATT3 <= 0.0")
//    x1Filter
  }
}
