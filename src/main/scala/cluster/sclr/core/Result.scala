package cluster.sclr.core

case class Result(dimensions: Vector[Int], rows: Vector[Int], coefficients: Vector[Double], error: Double, kDNF: String)
