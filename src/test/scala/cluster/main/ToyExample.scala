package cluster.main

import cluster.sclr.core.{Point, SetCover}
import weka.core.DenseInstance

object ToyExample {
  def main(args: Array[String]): Unit = {
    val p1 = Point(new DenseInstance(0), 0.0)
    val p2 = Point(new DenseInstance(0), 0.0)
    val p3 = Point(new DenseInstance(0), 0.0)
    val p4 = Point(new DenseInstance(0), 0.0)
    val p5 = Point(new DenseInstance(0), 0.0)
    val p6 = Point(new DenseInstance(0), 0.0)
    val p7 = Point(new DenseInstance(0), 0.0)
    val p2red = Point(new DenseInstance(0), 1.0)
    val p4red = Point(new DenseInstance(0), 1.0)
    val p5red = Point(new DenseInstance(0), 1.0)
    val p7red = Point(new DenseInstance(0), 1.0)

    val red = Set(p2red, p4red, p5red, p7red)

    val raining = Set(p1,p3,p4,p5,p6,p4red,p5red)

    val notRaining = Set(p2,p7,p2red,p7red)

    val sleepwell = Set(p2,p3,p4,p5,p7,p2red,p4red,p5red,p7red)

    val notSleepwell = Set(p1, p6)

    val inside = Set(p4, p5, p7, p4red, p5red, p7red)

    val notInside = Set(p1,p2,p3,p6,p2red)
    val list = Set(raining, notRaining, sleepwell, notSleepwell, inside, notInside)

    val lowdeg = SetCover.lowDegPartial(11, list, 6.0 / 11, 2.0, 7)
    System.out.println("error rate " + SetCover.errorRate(11, lowdeg))
  }
}
