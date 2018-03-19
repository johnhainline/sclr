package cluster.sclr.core.kdnf

import cluster.sclr.core.{Dataset, Result}

abstract class KdnfStrategy {
  def run(dataset: Dataset, yDimensions: Vector[Int], rows: Vector[Int]): Option[Result]
}
