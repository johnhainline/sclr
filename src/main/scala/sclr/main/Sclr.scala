package sclr.main

import akka.actor.{ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.cluster.Cluster
import akka.pattern.BackoffSupervisor
import akka.stream.ActorMaterializer
import sclr.core.actors.{ComputeActor, ManageActor}
import sclr.core.database.DatabaseDao
import sclr.core.http.InfoService

import scala.concurrent.duration._
import scala.language.postfixOps

object Sclr {

  def runResumeSupervisorForActor(name: String, props: Props)(implicit system: ActorSystem): ActorRef = {
    val supervisor = BackoffSupervisor.propsWithSupervisorStrategy(
        childProps = props,
        childName = name,
        minBackoff = 3 seconds,
        maxBackoff = 30 seconds,
        randomFactor = 0.2, // adds 20% "noise" to vary the intervals slightly
        strategy = OneForOneStrategy() {
          case e: Exception ⇒
            system.log.error(e, message = s"$name raised exception. Resuming...")
            SupervisorStrategy.Resume
        })
    system.actorOf(supervisor, name = s"${name}Supervisor")
  }

  def run(parallel: Int = 1): ActorSystem = {
    implicit val system: ActorSystem = ActorSystem("sclr")
    implicit val mat: ActorMaterializer = ActorMaterializer()(system)
    Cluster(system) registerOnMemberUp {
      val cluster = Cluster(system)
      val roles = cluster.getSelfRoles
      system.log.info(s"Member ${cluster.selfUniqueAddress} up. Contains roles: $roles")

      val dao = new DatabaseDao()
      if (roles.contains("compute")) {
        runResumeSupervisorForActor(name = "compute", ComputeActor.props(parallel, dao))
      }
      if (roles.contains("manage")) {
        runResumeSupervisorForActor(name = "manage", ManageActor.props(new InfoService(), dao))
      }
    }

    Cluster(system).registerOnMemberRemoved {
      val status = -1
      // exit JVM when ActorSystem has been terminated
      system.registerOnTermination(System.exit(status))
      // in case ActorSystem shutdown takes longer than 10 seconds, exit the JVM forcefully anyway
      system.scheduler.scheduleOnce(delay = 10 seconds)(System.exit(status))(system.dispatcher)
      // shut down ActorSystem
      system.terminate()
    }

    system
  }

  def main(args: Array[String]): Unit = {
    run()
  }
}