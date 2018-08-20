package sclr.main

import java.io.{BufferedWriter, File, FileWriter}

import sclr.core.database.DatabaseDao

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object RunComparison {
  def main(args: Array[String]): Unit = {

    val datasetName = "space_train2"

    for (i <- 1 to 2) {
      val filename = s"$datasetName-$i.txt"
      val file = new File(filename)
      if (file.exists()) {
        file.delete()
      }

      for (mu <- 0.2 to 0.4 by 0.2) {
        for (subset <- 150 to 120 by -10) {
          val bw = new BufferedWriter(new FileWriter(new File(filename), true))
          val json = s"""{"name":"$datasetName","dnfSize":2,"useLPNorm":true,"optionalSubset":$subset,"mu":$mu}"""
          bw.write(json)
          bw.write("\n")
          val system = Sclr.run(Array[String]("-w", json, "-p", "4", "-k"))
          Await.result(system.whenTerminated, Duration.Inf)
          val result = {
            val xa = DatabaseDao.makeSingleTransactor()
            val db = new DatabaseDao()
            db.getBestResult(xa, datasetName, yDimensions = 2, rows = 2)
          }
          bw.write(result.toString)
          bw.write("\n\n")
          bw.close()
          Thread.sleep(2000)
        }
      }
    }
  }
}
