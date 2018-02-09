package cluster.main

import cluster.sclr.core.SetCover
import weka.core.{DenseInstance, Instance}

object ToyExample {

  private def createInstance(value: Double): Instance = {
    val instance = new DenseInstance(1)
    instance.setValue(0, value)
    instance
  }

  def main(args: Array[String]): Unit = {
    val p1 = createInstance(0.0)
    val p2 = createInstance(0.0)
    val p3 = createInstance(0.0)
    val p4 = createInstance(0.0)
    val p5 = createInstance(0.0)
    val p6 = createInstance(0.0)
    val p7 = createInstance(0.0)
    val p2red = createInstance(1.0)
    val p4red = createInstance(1.0)
    val p5red = createInstance(1.0)
    val p7red = createInstance(1.0)

    val red = Set(p2red, p4red, p5red, p7red)

    val raining = Set(p1, p3, p4, p5, p6, p4red, p5red)

    val notRaining = Set(p2, p7, p2red, p7red)

    val sleepwell = Set(p2, p3, p4, p5, p7, p2red, p4red, p5red, p7red)

    val notSleepwell = Set(p1, p6)

    val inside = Set(p4, p5, p7, p4red, p5red, p7red)

    val notInside = Set(p1, p2, p3, p6, p2red)
    val list = Set(raining, notRaining, sleepwell, notSleepwell, inside, notInside)

    val lowdeg = SetCover.lowDegPartial(11, list, 6.0 / 11, 2.0, 7)
    System.out.println("error rate " + SetCover.errorRate(11, lowdeg))
  }
}
