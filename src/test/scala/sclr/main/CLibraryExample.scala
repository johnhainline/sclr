package sclr.main

import sclr.core.Messages.{Work, Workload}
import sclr.core.database.{DatabaseDaoHelper, Result}
import sclr.core.strategy.L2NormFastWrapper

import scala.util.Random

object CLibraryExample {

  // Main method to test our native library
  def main(args: Array[String]): Unit = {
    val wrapper = new L2NormFastWrapper()
    val helper = new DatabaseDaoHelper(new Random(1234))
    val dataset = helper.fakeDataset(size = 50, xLength = 10, yLength = 6)
    val workload = Workload("name", 2, 0.246, useLPNorm = true, None, Some(10), Some(1234))
    wrapper.prepare(dataset, workload)
    val expected = Result(1, Array(5,6), Array(3,4), Array(1.23,4.56), Some(1.0), Some("blah"))
    println(expected)
    val result = wrapper.run(Work(expected.index, expected.dimensions, expected.rows))
    println(result)
  }
}
