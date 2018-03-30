package bench

import org.scalameter.Bench.Forked
import org.scalameter.picklers.noPickler._
import org.scalameter.reporting.RegressionReporter
import org.scalameter.{Aggregator, Measurer}

abstract class CustomBench extends Forked[Double] {
  def aggregator: Aggregator[Double] = Aggregator.average
  def measurer: Measurer[Double] = new Measurer.IgnoringGC
    with Measurer.PeriodicReinstantiation[Double]
    with Measurer.OutlierElimination[Double]
    with Measurer.RelativeNoise {
    def numeric: Numeric[Double] = implicitly[Numeric[Double]]
  }
  def tester = RegressionReporter.Tester.Accepter()

}
