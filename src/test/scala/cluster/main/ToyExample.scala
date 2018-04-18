package cluster.main

import cluster.sclr.core.{SetCover, XYZ}

import scala.collection.immutable.BitSet

object ToyExample {

  def main(args: Array[String]): Unit = {
    val idToRedness = Map(
      1 -> 0.0,
      2 -> 0.0,
      3 -> 0.0,
      4 -> 0.0,
      5 -> 0.0,
      6 -> 0.0,
      7 -> 0.0,
      8 -> 1.0,
      9 -> 1.0,
      10 -> 1.0,
      11 -> 1.0
    )
    val p1 = XYZ(1, Array.empty, Array.empty, 0.0)
    val p2 = XYZ(2, Array.empty, Array.empty, 0.0)
    val p3 = XYZ(3, Array.empty, Array.empty, 0.0)
    val p4 = XYZ(4, Array.empty, Array.empty, 0.0)
    val p5 = XYZ(5, Array.empty, Array.empty, 0.0)
    val p6 = XYZ(6, Array.empty, Array.empty, 0.0)
    val p7 = XYZ(7, Array.empty, Array.empty, 0.0)
    val p2red = XYZ(8, Array.empty, Array.empty, 0.0)
    val p4red = XYZ(9, Array.empty, Array.empty, 0.0)
    val p5red = XYZ(10, Array.empty, Array.empty, 0.0)
    val p7red = XYZ(11, Array.empty, Array.empty, 0.0)

    val raining = BitSet(p1.id, p3.id, p4.id, p5.id, p6.id, p4red.id, p5red.id)

    val notRaining = BitSet(p2.id, p7.id, p2red.id, p7red.id)

    val sleepwell = BitSet(p2.id, p3.id, p4.id, p5.id, p7.id, p2red.id, p4red.id, p5red.id, p7red.id)

    val notSleepwell = BitSet(p1.id, p6.id)

    val inside = BitSet(p4.id, p5.id, p7.id, p4red.id, p5red.id, p7red.id)

    val notInside = BitSet(p1.id, p2.id, p3.id, p6.id, p2red.id)
    val allDnfs = Vector(raining, notRaining, sleepwell, notSleepwell, inside, notInside)

    val setCoverSimple = new SetCover(allDnfs, 6.0 / 11, 7, true)
    val lowdegSimple = setCoverSimple.lowDegPartial2(idToRedness)
    System.out.println("error rate " + lowdegSimple._2)

    val setCoverComplex = new SetCover(allDnfs, 6.0 / 11, 7, false)
    val lowdegComplex = setCoverComplex.lowDegPartial2(idToRedness)
    System.out.println("error rate " + lowdegComplex._2)
  }
}
