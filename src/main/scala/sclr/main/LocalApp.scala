package sclr.main

object LocalApp {
  def main(args: Array[String]): Unit = {
//    val json = """{"name":"tiny","dnfSize":2,"useLPNorm":true,"mu":0.24}""" //  (5,10), (1,-6), (2,7), (-2,-4)
//    val json = """{"name":"m500","dnfSize":2,"optionalSubset":100,"useLPNorm":true,"mu":0.24}""" // (-2,-4), (5,10), (-3,7), (1,-6)
//    val json = """{"name":"m1000","dnfSize":2,"optionalSubset":100,"useLPNorm":true,"mu":0.24}""" // (-2,-4), (5,10), (-3,7), (1,-6)
//    val json = """{"name":"m10000","dnfSize":2,"useLPNorm":true,"optionalSubset":500,"mu":0.246}"""
    val json = """{"name":"m5000_noise","dnfSize":2,"useLPNorm":false,"optionalSubset":500,"optionalEpsilon": 1.4,"mu":0.246}"""
    Sclr.run(Array[String]("-w", json, "-k"))
  }
}
