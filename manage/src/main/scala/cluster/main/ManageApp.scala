package cluster.main

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import akka.stream.ActorMaterializer
import cluster.sclr.actors.ManageActor
import cluster.sclr.core.DatabaseDao
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps

object ManageApp {
  def main(args: Array[String]): Unit = {
    val appConfig = ConfigFactory.load()
    val config = ConfigFactory.parseString("akka.cluster.roles = [manage]").
      withFallback(appConfig)

    implicit val system: ActorSystem = ActorSystem("sclr", config)
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val dao = new DatabaseDao()
    system.actorOf(ManageActor.props(dao), name = "manage")

//    system.log.info(s"System will start when all roles have been filled by nodes in the cluster.")
//    Cluster(system).registerOnMemberUp {
//      system.actorOf(ManageActor.props(new DatabaseDao()), "manage")
//    }

    Cluster(system).registerOnMemberRemoved {
      val status = -1
      // exit JVM when ActorSystem has been terminated
      system.registerOnTermination(System.exit(status))
      // in case ActorSystem shutdown takes longer than 10 seconds, exit the JVM forcefully anyway
      system.scheduler.scheduleOnce(10 seconds)(System.exit(status))(system.dispatcher)
      // shut down ActorSystem
      system.terminate()
    }
  }
}
