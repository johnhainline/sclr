package cluster.sclr.core.strategy

import cluster.sclr.core.{DatabaseDaoHelper, XYZ}
import org.scalameter.api._
import org.scalameter.picklers.Implicits._

import scala.collection.immutable.BitSet
import scala.collection.mutable.ListBuffer
import scala.util.Random

object L2NormFilterBench extends Bench.OfflineReport {

  val random = new Random(1234)
  val database = new DatabaseDaoHelper(random)

  val start = Math.pow(2,14).toInt
  val end = Math.pow(2,18).toInt
  val dataLengthGen = Gen.exponential("dataLength")(start, end, 2)
  val xLength = 10

  val selections = for(dataLength <- dataLengthGen) yield {
    (database.fakeDataset(dataLength, xLength, 6), randomSelections(xLength, 3))
  }

  private def randomSelections(xLength: Int, selectionCount: Int) = {
    val range = (0 until xLength).toList
    val selections = random.shuffle(range).take(selectionCount)
    selections.map((_, random.nextBoolean())).toVector
  }


  private def standardFilter(data: Array[XYZ], selections: Vector[(Int, Boolean)]) = {
    val ids = data.filter { xyz =>
      selections.map(selection => xyz.x(selection._1) == selection._2).forall(identity)
    }.map(_.id)
    BitSet(ids:_*)
  }

  private def foldFilter(data: Array[XYZ], selections: Vector[(Int, Boolean)]) = {
    val ids = data.foldLeft(new ListBuffer[Int]()) { (l,xyz) =>
      if (selections.map(selection => xyz.x(selection._1) == selection._2).forall(identity)) {
        l.append(xyz.id)
      }
      l
    }
    BitSet(ids:_*)
  }

  private def collectFilter(data: Array[XYZ], selections: Vector[(Int, Boolean)]) = {
    val mapFilter = new PartialFunction[XYZ, Int] {
      def apply(xyz: XYZ) = xyz.id
      def isDefinedAt(xyz: XYZ) = selections.map(selection => xyz.x(selection._1) == selection._2).forall(identity)
    }
    val ids = data.collect(mapFilter)
    BitSet(ids:_*)
  }

  performance of "L2Norm" config (
    exec.benchRuns -> 100
    ) in {
    measure method "standardFilter" in {
      using (selections) in { case (dataset, selection) =>
        standardFilter(dataset.data, selection)
      }
    }
    measure method "foldFilter" in {
      using (selections) in { case (dataset, selection) =>
        foldFilter(dataset.data, selection)
      }
    }
    measure method "collectFilter" in {
      using (selections) in { case (dataset, selection) =>
        collectFilter(dataset.data, selection)
      }
    }
  }
}
