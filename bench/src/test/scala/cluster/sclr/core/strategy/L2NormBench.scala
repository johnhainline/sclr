package cluster.sclr.core.strategy

import cluster.sclr.core.{DatabaseDaoHelper, XYZ}
import org.scalameter.api._
import org.scalameter.picklers.Implicits._

import scala.collection.immutable.BitSet
import scala.collection.mutable.ListBuffer
import scala.util.Random

object L2NormBench extends Bench.OfflineReport {

  val random = new Random(1234)
  val database = new DatabaseDaoHelper(random)

//  val start = Math.pow(2,12).toInt
//  val end = Math.pow(2,15).toInt
//  val dataLengthRange = Gen.exponential("dataLength")(start, end, 2)
//val dataLengthRange = Gen.single("dataLength")(10000)
  val dataLengthRange = Gen.range("dataLength")(5000,20000,5000)
//  val xLengthRange = Gen.range("xLength")(4, 20, 4)
  val xLength = 10
//  val selectionRange = Gen.range("selection")(0, 10, 1)

  val datasets = for(dataLength <- dataLengthRange) yield {
    database.fakeDataset(dataLength, xLength, 6)
  }

  val selections = for (dataset <- datasets) yield {
    (dataset, randomSelections(xLength))
  }

  private def randomSelections(xLength: Int) = {
    Vector((random.nextInt(xLength), random.nextBoolean()), (random.nextInt(xLength), random.nextBoolean()))
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

  performance of "L2Norm" config (
    exec.benchRuns -> 15
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
  }
}
