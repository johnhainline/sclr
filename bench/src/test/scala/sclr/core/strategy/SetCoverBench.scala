//package sclr.core.strategy
//
//import java.util.concurrent.TimeUnit
//
//import org.openjdk.jmh.annotations.{BenchmarkMode, Mode, OutputTimeUnit}
//import sclr.core.database.{DatabaseDaoHelper, XYZ}
//import org.scalameter.api._
//
//import scala.collection.immutable.BitSet
//import scala.collection.mutable.ListBuffer
//import scala.util.Random
//
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@BenchmarkMode(Array(Mode.Throughput))
//class SetCoverBench {
//
//  val random = new Random(1234)
//  val database = new DatabaseDaoHelper(random)
//
//  val start = Math.pow(2,14).toInt
//  val end = Math.pow(2,18).toInt
//  val dataLengthGen = Gen.exponential("dataLength")(start, end, 2)
//  val xLength = 10
//  val setCover = new SetCover(allTerms, mu, beta, simpleAlgorithm = true)
//
//  val selections = for(dataLength <- dataLengthGen) yield {
//    (database.fakeDataset(dataLength, xLength, 6), randomSelections(xLength, 3))
//  }
//
//  def
//
//  performance of "L2Norm" config (
//    exec.benchRuns -> 10
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
