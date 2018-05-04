package cluster.main

import akka.actor.ActorSystem
import akka.cluster.Cluster
import cluster.sclr.actors.ComputeActor
import cluster.sclr.core.DatabaseDao
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps

object ComputeApp {
  def main(args: Array[String]): Unit = {
    val appConfig = ConfigFactory.load()
    val config = ConfigFactory.parseString("akka.cluster.roles = [compute]").
      withFallback(appConfig)

    val system = ActorSystem("sclr", config)
    val dao = new DatabaseDao()
    val parallel = 1
    system.actorOf(ComputeActor.props(parallel, dao), name = "compute")

//    system.log.info(s"System will start when all roles have been filled by nodes in the cluster.")
//    Cluster(system) registerOnMemberUp {
//      val supervisor = BackoffSupervisor.props(
//        Backoff.onFailure(
//          ComputeActor.props(new DatabaseDao()),
//          childName = "compute",
//          minBackoff = 3.seconds,
//          maxBackoff = 30.seconds,
//          randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
//        ).withAutoReset(5.seconds))
//      system.actorOf(supervisor)
//    }

    Cluster(system).registerOnMemberRemoved {
      val status = -1
      // exit JVM when ActorSystem has been terminated
      system.registerOnTermination(System.exit(status))
      // in case ActorSystem shutdown takes longer than 10 seconds, exit the JVM forcefully anyway
      system.scheduler.scheduleOnce(delay = 10 seconds)(System.exit(status))(system.dispatcher)
      // shut down ActorSystem
      system.terminate()
    }
  }
}
