package cluster.main

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import akka.stream.ActorMaterializer
import cluster.sclr.actors.{FrontendActor, ManageActor}
import cluster.sclr.core.DatabaseDao
import cluster.sclr.http.InfoService
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps

object FrontendApp {
  def main(args: Array[String]): Unit = {
    val appConfig = ConfigFactory.load()
    val config = ConfigFactory.parseString("akka.cluster.roles = [frontend]").
      withFallback(appConfig)

    implicit val system: ActorSystem = ActorSystem("sclr", config)
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    system.log.info(s"System will start when all roles have been filled by nodes in the cluster.")

    Cluster(system).registerOnMemberUp {
      system.actorOf(Props(new Terminator()), "terminator")
      system.actorOf(FrontendActor.props(new InfoService()), "frontend")
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
