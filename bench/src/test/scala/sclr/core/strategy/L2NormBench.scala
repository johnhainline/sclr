package sclr.core.strategy

import java.util.concurrent.TimeUnit

import combinations.Combinations
import combinations.iterators.MultipliedIterator
import org.openjdk.jmh.annotations._
import sclr.core.Messages.{Work, Workload}
import sclr.core.database.FakeDatabaseDao

import scala.util.Random

@State(Scope.Benchmark)
object L2NormBench {
  val random = new Random(1234)
  val database = new FakeDatabaseDao(random)
  val workload = Workload("test", 2, 0.24, useLPNorm = true)

  val dataset = database.fakeDataset(20, xLength = 10, yLength = 6)
  val selectYDimensions = () => Combinations(dataset.yLength, 2).iterator()
  val selectRows = () => Combinations(dataset.data.length, workload.getRowsConstant()).iterator()
  val getIterator = () => MultipliedIterator(Vector(selectYDimensions, selectRows)).zipWithIndex.map { case (next, index) =>
    Work(index, selectedDimensions = next.head, selectedRows = next.last)
  }
  val l2Norm = new L2Norm(L2NormBench.dataset, L2NormBench.workload)
  val l2NormSetCover = new L2NormSetCover(L2NormBench.dataset, L2NormBench.workload)
}

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.AverageTime))
class L2NormBench {
  import L2NormBench._

  @Benchmark
  def l2NormStartupBench(): Unit = {
    val l2Norm = new L2Norm(L2NormBench.dataset, L2NormBench.workload)
    Unit
  }

  @Benchmark
  def l2NormSetCoverStartupBench(): Unit = {
    val l2NormSetCover = new L2NormSetCover(L2NormBench.dataset, L2NormBench.workload)
    Unit
  }

  @Benchmark
  def l2NormBench(): Unit = {
    val iterator = getIterator()
    for (combo <- iterator) {
      l2Norm.run(combo)
    }
  }

  @Benchmark
  def l2NormSetCoverBench(): Unit = {
    val iterator = getIterator()
    for (combo <- iterator) {
      l2NormSetCover.run(combo)
    }
  }
}
