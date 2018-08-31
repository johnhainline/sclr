package sclr.main

import sclr.core.Messages.Work
import sclr.core.database.Result
import sclr.core.strategy.L2NormFastWrapper

object CLibraryExample {

  // Main method to test our native library
  def main(args: Array[String]): Unit = {
    val wrapper = new L2NormFastWrapper()
    wrapper.prepare(null, "name", 2, 0.25, 23.4, 200, 1)
    val expected = Result(1, Array(5,6), Array(3,4), Array(1.23,4.56), Some(1.0), Some("blah"))
    println(expected)
    val result = wrapper.run(Work(expected.index, expected.dimensions, expected.rows))
    println(result)
  }
}
