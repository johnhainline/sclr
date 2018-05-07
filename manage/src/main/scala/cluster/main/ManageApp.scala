package cluster.main

import akka.actor.{ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.cluster.Cluster
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.stream.ActorMaterializer
import cluster.sclr.actors.{ComputeActor, ManageActor}
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

    Cluster(system) registerOnMemberUp {
      val supervisor = BackoffSupervisor.props(
        Backoff.onFailure(
          ManageActor.props(new DatabaseDao()),
          childName = "manage",
          minBackoff = 3.seconds,
          maxBackoff = 30.seconds,
          randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
        ).withAutoReset(10.seconds) // reset if the child does not throw any errors within 10 seconds
          .withSupervisorStrategy(
          OneForOneStrategy() {
            case e: Exception ⇒ {
              system.log.warning(s"ManageActor died. Restarting. (exception:$e)")
              SupervisorStrategy.Restart
            }
          }))
      system.actorOf(supervisor, "manageSupervisor")
    }

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
