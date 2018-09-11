package sclr.core.strategy

import java.util.concurrent.TimeUnit

import combinations.Combinations
import combinations.iterators.MultipliedIterator
import org.openjdk.jmh.annotations._
import sclr.core.Messages.{Work, Workload}
import sclr.core.database.DatabaseDaoHelper

import scala.util.Random

@State(Scope.Benchmark)
object L2NormBench {
  val random = new Random(1234)
  val database = new DatabaseDaoHelper(random)
  val workload = Workload("test", 2, 0.24, useLPNorm = true)

  val dataset = database.fakeDataset(20, xLength = 10, yLength = 6)
  val selectYDimensions = () => Combinations(dataset.yLength, 2).iterator()
  val selectRows = () => Combinations(dataset.data.length, workload.getRowsConstant()).iterator()
  val getIterator = () => MultipliedIterator(Vector(selectYDimensions, selectRows)).zipWithIndex.map { case (next, index) =>
    Work(index, selectedDimensions = next.head, selectedRows = next.last)
  }
}

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.Throughput))
class L2NormBench {

  @Benchmark
  def l2NormBench(): Unit = {
    for (combo <- L2NormBench.getIterator()) {
      val strategy = new L2Norm(L2NormBench.dataset, L2NormBench.workload, simpleAlgorithm = true)
      strategy.run(combo)
    }
  }

  @Benchmark
  def l2NormFastBench(): Unit = {
    for (combo <- L2NormBench.getIterator()) {
      val strategy = new L2NormFast(L2NormBench.dataset, L2NormBench.workload)
      strategy.run(combo)
    }
  }
}
