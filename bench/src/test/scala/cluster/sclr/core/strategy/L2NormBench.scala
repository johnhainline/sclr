package cluster.sclr.core.strategy

import cluster.sclr.Messages.Workload
import cluster.sclr.core.DatabaseDaoHelper
import combinations.{CombinationAggregation, CombinationBuilder}
import org.scalameter.api._
import org.scalameter.picklers.Implicits._

import scala.util.Random

object L2NormBench extends Bench.OfflineReport {

  val random = new Random(1234)
  val database = new DatabaseDaoHelper(random)
  val workload = Workload("test", 2, 0.24, useLPNorm = true)
  val l2NormSimple  = new L2Norm(workload, simpleAlgorithm = true)
  val l2NormComplex = new L2Norm(workload, simpleAlgorithm = false)

  val dataLengthGen = Gen.single("dataLength")(200)
  val generator = for(dataLength <- dataLengthGen) yield {
    val dataset = database.fakeDataset(dataLength, 10, 6)
    val selectYDimensions = CombinationBuilder(dataset.yLength, 2)
    val selectRows = CombinationBuilder(dataset.data.length, workload.getRowsConstant())
    val iterator = CombinationAggregation(Vector(selectYDimensions,selectRows)).all()
    (dataset, iterator)
  }

//  performance of "L2Norm" config (
//    exec.benchRuns -> 10,
//    exec.minWarmupRuns -> 1,
//    exec.maxWarmupRuns -> 1
//    ) in {
//    measure method "simpleAlgorithm" in {
//      using (generator) in { case (dataset, iterator) =>
//        for (combo <- iterator) {
//          l2NormSimple.run(dataset, combo.head, combo.last)
//        }
//      }
//    }
//    measure method "complexAlgorithm" in {
//      using (generator) in { case (dataset, iterator) =>
//        for (combo <- iterator) {
//          l2NormComplex.run(dataset, combo.head, combo.last)
//        }
//      }
//    }
//  }
}

