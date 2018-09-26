package sclr.core.strategy

import java.util.concurrent.TimeUnit

import combinations.Combinations
import combinations.iterators.MultipliedIterator
import org.openjdk.jmh.annotations._
import sclr.core.Messages.{Work, Workload}
import sclr.core.database.FakeDatabaseDao

import scala.util.Random

@State(Scope.Benchmark)
object SupNormBench {
  val random = new Random(1234)
  val database = new FakeDatabaseDao(random)
  val workload = Workload("test", 2, 0.24, useLPNorm = false)

  val dataset = database.fakeDataset(20, xLength = 10, yLength = 6)
  val selectYDimensions = () => Combinations(dataset.yLength, 2).iterator()
  val selectRows = () => Combinations(dataset.data.length, workload.getRowsConstant()).iterator()
  val getIterator = () => MultipliedIterator(Vector(selectYDimensions, selectRows)).zipWithIndex.map { case (next, index) =>
    Work(index, selectedDimensions = next.head, selectedRows = next.last)
  }
  val strategy = new SupNorm(dataset, workload)
}

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.AverageTime))
class SupNormBench {
  import SupNormBench._

  @Benchmark
  def supNormStartupBench(): Unit = {
    val strategy = new SupNorm(dataset, workload)
    Unit
  }

  @Benchmark
  def l2NormBench(): Unit = {
    val iterator = getIterator()
    for (combo <- iterator) {
      strategy.run(combo)
    }
  }
}
