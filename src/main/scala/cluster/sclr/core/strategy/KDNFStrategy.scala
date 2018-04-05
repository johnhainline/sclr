package cluster.sclr.core.strategy

import cluster.sclr.core.{Dataset, Result}

abstract class KDNFStrategy(dataset: Dataset) {
  def run(yDimensions: Vector[Int], rows: Vector[Int]): Option[Result]
}
