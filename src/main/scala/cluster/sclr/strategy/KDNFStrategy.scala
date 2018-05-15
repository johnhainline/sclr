package cluster.sclr.strategy

import cluster.sclr.database.{Dataset, Result}

abstract class KDNFStrategy(dataset: Dataset) {
  def run(yDimensions: Vector[Int], rows: Vector[Int]): Result
}
