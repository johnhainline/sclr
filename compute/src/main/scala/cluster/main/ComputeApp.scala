package cluster.main

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import cluster.sclr.actors.ComputeActor
import cluster.sclr.doobie.ResultsDao
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps

object ComputeApp {
  def main(args: Array[String]): Unit = {
//    val internalIp = NetworkConfig.hostLocalAddress
    val appConfig = ConfigFactory.load()
    val config = ConfigFactory.parseString("akka.cluster.roles = [compute]").
//      withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.bind-hostname=$internalIp")).
      withFallback(appConfig)

    val system = ActorSystem("sclr", config)
    system.log.info(s"System will start when all roles have been filled by nodes in the cluster.")

    Cluster(system) registerOnMemberUp {
      system.actorOf(Props(new Terminator()), "terminator")
      system.actorOf(ComputeActor.props(new ResultsDao()), "compute")
    }

    Cluster(system).registerOnMemberRemoved {
      // exit JVM when ActorSystem has been terminated
      system.registerOnTermination(System.exit(-1))
      // in case ActorSystem shutdown takes longer than 10 seconds, exit the JVM forcefully anyway
      system.scheduler.scheduleOnce(10 seconds)(System.exit(-1))(system.dispatcher)
      // shut down ActorSystem
      system.terminate()
    }
  }
}
