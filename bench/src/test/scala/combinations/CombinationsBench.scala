package combinations

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.{Benchmark, BenchmarkMode, Mode, OutputTimeUnit}

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.Throughput))
class CombinationsBench {

  @Benchmark
  def rank(): Unit = {
    Combinations.rank(Array(10,20))
  }

  @Benchmark
  def unrank(): Unit = {
    Combinations.unrank(k = 10, index = 990000)
  }
}



