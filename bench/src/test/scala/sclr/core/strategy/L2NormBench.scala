package sclr.core.strategy

import combinations.Combinations
import combinations.iterators.MultipliedIterator
import org.scalameter.api._
import org.scalameter.picklers.Implicits._
import sclr.core.Messages.{Work, Workload}
import sclr.core.database.DatabaseDaoHelper

import scala.util.Random

object L2NormBench extends Bench.OfflineRegressionReport {

  val random = new Random(1234)
  val database = new DatabaseDaoHelper(random)
  val workload = Workload("test", 2, 0.24, useLPNorm = true)

  val dataLengthGen = Gen.single(axisName = "dataLength")(200)
  val generator = for(dataLength <- dataLengthGen) yield {
    val dataset = database.fakeDataset(dataLength, xLength = 10, yLength = 6)
    val l2Norm  = new L2Norm(dataset, workload, simpleAlgorithm = true)
    val selectYDimensions = () => Combinations(dataset.yLength, 2).iterator()
    val selectRows = () => Combinations(dataset.data.length, workload.getRowsConstant()).iterator()
    val iterator = MultipliedIterator(Vector(selectYDimensions, selectRows)).zipWithIndex.map { case (next, index) =>
      Work(index, selectedDimensions = next.head, selectedRows = next.last)
    }
    (l2Norm, dataset, iterator)
  }

  performance of "L2Norm" config (
    exec.benchRuns -> 10,
    exec.minWarmupRuns -> 1,
    exec.maxWarmupRuns -> 1
    ) in {
    measure method "standard" in {
      using (generator) in { case (l2Norm, dataset, iterator) =>
        for (combo <- iterator) {
          l2Norm.run(combo)
        }
      }
    }
  }
}

