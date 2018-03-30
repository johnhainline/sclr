//package cluster.sclr.core.strategy
//
//import cluster.sclr.Messages.Workload
//import cluster.sclr.core.{DatabaseDaoHelper, XYZ}
//import org.scalameter.api._
//import org.scalameter.picklers.Implicits._
//
//import scala.util.Random
//
//class L2NormBench extends Bench.OfflineReport {
//
//  val random = new Random(1234)
//  val database = new DatabaseDaoHelper(random)
//  val testObject = new L2Norm(Workload("test", 3, 0.24, useLPNorm = true))
//
//  val selections = for(dataLength <- dataLengthGen) yield {
//    (database.fakeDataset(dataLength, xLength, 6), randomSelections(xLength, 3))
//  }
//
//  private def randomSelections(xLength: Int, selectionCount: Int) = {
//    val range = (0 until xLength).toList
//    val selections = random.shuffle(range).take(selectionCount)
//    selections.map((_, random.nextBoolean())).toVector
//  }
//
//  performance of "L2Norm" config (
//    ) in {
//    measure method "standardFilter" in {
//      using (selections) in { case (dataset, selection) =>
//        standardFilter(dataset.data, selection)
//      }
//    }
//    measure method "foldFilter" in {
//      using (selections) in { case (dataset, selection) =>
//        foldFilter(dataset.data, selection)
//      }
//    }
//    measure method "collectFilter" in {
//      using (selections) in { case (dataset, selection) =>
//        collectFilter(dataset.data, selection)
//      }
//    }
//  }
//}
//
