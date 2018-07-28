package sclr.main

object LocalApp {
  def main(args: Array[String]): Unit = {
//    val json = """{"name":"tiny","dnfSize":2,"optionalSubset":10,"useLPNorm":true,"mu":0.2, "optionalRandomSeed":1}"""
    val json = """{"name":"boston_train2","dnfSize":2,"useLPNorm":true,"mu":0.2}"""
    Sclr.run(Array[String]("-w", json, "-p", "4"))

//    Run with different sampling values from 150 to 50 (step = -10).
//    Mu varies from 0.2 to 0.6 (step = 0.2).

    //    val sleep = 3000
//    Thread.sleep(sleep)
//    system.actorSelection("/user/computeSupervisor") ! Kill
//    Thread.sleep(sleep)
//    Sclr.runResumeSupervisorForActor(name = "compute", ComputeActor.props(1, new DatabaseDao()))
//    Thread.sleep(sleep)
//    system.actorSelection("/user/computeSupervisor") ! Kill
//    Thread.sleep(sleep)
//    Sclr.runResumeSupervisorForActor(name = "compute", ComputeActor.props(4, new DatabaseDao()))
  }
}
