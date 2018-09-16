package sclr.main

import combinations.Combinations
import combinations.iterators.MultipliedIterator
import sclr.core.Messages.{Work, Workload}
import sclr.core.database.FakeDatabaseDao
import sclr.core.strategy.{KDNFStrategy, L2Norm, L2NormFast, L2NormSetCover}

import scala.util.Random

object L2NormFastExample {

  def time[R](block: => R): (R, Double) = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    val nanos = t1 - t0
    (result, nanos)
  }

  val dnfSize = 2
  val random = new Random(1234)
  val database = new FakeDatabaseDao(random)

  def printInfo(size: Int, xLength: Int, yLength: Int): Int = {
    val sizeYDim = Combinations(yLength, dnfSize).size
    val sizeRows = Combinations(size, 2).size
    val total = sizeYDim * sizeRows
    val terms = xLength * dnfSize * 4
    println(s"Size: $size, xLength: $xLength, yLength: $yLength")
    println(s"terms ($xLength * $dnfSize * 4): $terms")
    println(s"yDim combos ($yLength choose 2): $sizeYDim")
    println(s"row combos ($size choose 2): $sizeRows")
    println(s"$total items, $terms terms")
    total.toInt
  }

  def runDataset(size: Int, xLength: Int, yLength: Int): Unit = {
    val totalWorkItems = printInfo(size, xLength, yLength)
    val workload = Workload("test", dnfSize, 0.24, useLPNorm = true)
    val dataset = database.fakeDataset(size, xLength, yLength)
    val l2NormSetCover:KDNFStrategy = new L2NormSetCover(dataset, workload)
    val l2Norm:KDNFStrategy = new L2Norm(dataset, workload)
    val selectYDimensions = () => Combinations(dataset.yLength, 2).iterator()
    val selectRows = () => Combinations(dataset.data.length, workload.getRowsConstant()).iterator()
    val iterator = () => MultipliedIterator(Vector(selectYDimensions, selectRows)).zipWithIndex.map { case (next, index) =>
      Work(index, selectedDimensions = next.head, selectedRows = next.last)
    }

    for (strategy <- List(l2Norm, l2NormSetCover)) {
      val (result, nanos) = time {
        val i = iterator()
        for (work <- i) {
          strategy.run(work)
        }
      }

      val avgTime = (nanos.toDouble / 1000000.0) / totalWorkItems.toDouble
      val totalTime = nanos.toDouble / 1000000000.0
      println(strategy)
      println("TIME\ntotal: %8f s".format(totalTime))
      println("average:%.8f ms\n".format(avgTime))
    }
  }

  def main(args: Array[String]): Unit = {
//    runDataset(size = 80, xLength = 5,  yLength = 4)
//    runDataset(size = 80, xLength = 10, yLength = 4)
//    runDataset(size = 80, xLength = 20, yLength = 4)
//    runDataset(size = 80, xLength = 40, yLength = 4)
//    runDataset(size = 100, xLength = 5, yLength = 4)
//    runDataset(size = 120, xLength = 5, yLength = 4)
    runDataset(size = 120, xLength = 40, yLength = 4)
  }
}
