package sclr.core.strategy

import combinations.Combinations
import combinations.iterators.MultipliedIterator
import sclr.core.Messages.{Work, Workload}
import sclr.core.database.DatabaseDaoHelper

import scala.util.Random

object L2NormSimpleMain {

  def time[R](block: => R): (R, Double) = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    val nanos = t1 - t0
    (result, nanos)
  }

  val dnfSize = 2
  val random = new Random(1234)
  val database = new DatabaseDaoHelper(random)

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
    val l2Norm  = new L2Norm(dataset, workload, simpleAlgorithm = true)
    val selectYDimensions = () => Combinations(dataset.yLength, 2).iterator()
    val selectRows = () => Combinations(dataset.data.length, workload.getRowsConstant()).iterator()
    val iterator = MultipliedIterator(Vector(selectYDimensions, selectRows)).zipWithIndex.map { case (next, index) =>
      Work(index, selectedDimensions = next.head, selectedRows = next.last)
    }

    val (result, nanos) = time {
      for (work <- iterator) {
        l2Norm.run(work)
      }
    }

    val avgTime = (nanos.toDouble / 1000000.0) / totalWorkItems.toDouble
    val totalTime = nanos.toDouble / 1000000000.0
    println("TIME\ntotal: %8f s".format(totalTime))
    println("average:%.8f ms\n".format(avgTime))
  }

  def main(args: Array[String]): Unit = {
    runDataset(size = 80, xLength = 5,  yLength = 4)
    runDataset(size = 80, xLength = 10, yLength = 4)
    runDataset(size = 80, xLength = 20, yLength = 4)
    runDataset(size = 80, xLength = 40, yLength = 4)
  }
}
