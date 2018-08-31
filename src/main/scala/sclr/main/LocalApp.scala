package sclr.main

object LocalApp {
  def main(args: Array[String]): Unit = {
    val json = """{"name":"m10000","dnfSize":2,"useLPNorm":true,"optionalSubset":500,"mu":0.246}"""
    Sclr.run(Array[String]("-w", json, "-k"))
  }
}
