package cluster.main

import cluster.sclr.core.{Point, SetCover, XYZ}

object ToyExample {

  def main(args: Array[String]): Unit = {
    val p1 = Point(XYZ(1, Array.empty, Array.empty, 0.0), 0.0)
    val p2 = Point(XYZ(2, Array.empty, Array.empty, 0.0), 0.0)
    val p3 = Point(XYZ(3, Array.empty, Array.empty, 0.0), 0.0)
    val p4 = Point(XYZ(4, Array.empty, Array.empty, 0.0), 0.0)
    val p5 = Point(XYZ(5, Array.empty, Array.empty, 0.0), 0.0)
    val p6 = Point(XYZ(6, Array.empty, Array.empty, 0.0), 0.0)
    val p7 = Point(XYZ(7, Array.empty, Array.empty, 0.0), 0.0)
    val p2red = Point(XYZ(8, Array.empty, Array.empty, 0.0), 1.0)
    val p4red = Point(XYZ(9, Array.empty, Array.empty, 0.0), 1.0)
    val p5red = Point(XYZ(10, Array.empty, Array.empty, 0.0), 1.0)
    val p7red = Point(XYZ(11, Array.empty, Array.empty, 0.0), 1.0)

    val red = Set(p2red, p4red, p5red, p7red)

    val raining = Set(p1, p3, p4, p5, p6, p4red, p5red)

    val notRaining = Set(p2, p7, p2red, p7red)

    val sleepwell = Set(p2, p3, p4, p5, p7, p2red, p4red, p5red, p7red)

    val notSleepwell = Set(p1, p6)

    val inside = Set(p4, p5, p7, p4red, p5red, p7red)

    val notInside = Set(p1, p2, p3, p6, p2red)
    val allDnfs = Set(raining, notRaining, sleepwell, notSleepwell, inside, notInside)

    val setCover = new SetCover(allDnfs, 6.0 / 11, 7)
    val lowdegSimple = setCover.simpleGreedy(2.0)
    System.out.println("error rate " + setCover.errorRate(lowdegSimple))
    val lowdegComplex = setCover.complexGreedy(2.0)
    System.out.println("error rate " + setCover.errorRate(lowdegComplex))
  }
}
