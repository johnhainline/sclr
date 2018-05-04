package cluster.main

import akka.actor.{ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.cluster.Cluster
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.stream.ActorMaterializer
import cluster.sclr.actors.{ComputeActor, FrontendActor, ManageActor}
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
      val supervisor = BackoffSupervisor.props(
        Backoff.onFailure(
          FrontendActor.props(new InfoService()),
          childName = "frontend",
          minBackoff = 3.seconds,
          maxBackoff = 30.seconds,
          randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
        ).withAutoReset(10.seconds) // reset if the child does not throw any errors within 10 seconds
          .withSupervisorStrategy(
          OneForOneStrategy() {
            case e: Exception ⇒ {
              system.log.warning(s"FrontendActor died. Restarting. (exception:$e)")
              SupervisorStrategy.Restart
            }
          }))
      system.actorOf(supervisor, "computeSupervisor")
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
