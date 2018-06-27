package sclr.main

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.stream.ActorMaterializer
import sclr.core.Messages.Workload
import sclr.core.actors.{ComputeActor, LifecycleActor, ManageActor}
import sclr.core.database.DatabaseDao
import sclr.core.http.SclrService

import scala.language.postfixOps

object Sclr {
  import org.rogach.scallop._

  class Conf(arguments: Array[String]) extends ScallopConf(arguments) {
    import spray.json._
    implicit val workloadConverter = singleArgConverter[Workload](str => SclrService.workloadFormat.read(str.parseJson))
    val workload = opt[Workload](descr = "manage: workload to immediately start with")
    val parallelization = opt[Int](descr = "compute: number of compute streams to instantiate", default = Some(1), validate = 0< )
    val count = opt[Int](descr = "compute: number of distinct pieces of work to run before exiting", validate = 0< )
    verify()
  }

  def main(args: Array[String]): Unit = {
    run(args)
  }

  def run(args: Array[String]): ActorSystem = {
    val conf = new Conf(args)
    implicit val system: ActorSystem = ActorSystem("sclr")
    implicit val mat: ActorMaterializer = ActorMaterializer()(system)
    Cluster(system) registerOnMemberUp {
      val cluster = Cluster(system)
      val roles = cluster.getSelfRoles
      system.log.info(s"Member ${cluster.selfUniqueAddress} up. Contains roles: $roles")

      val lifecycleActor = system.actorOf(LifecycleActor.props(), name = "lifecycle")

      val dao = new DatabaseDao()
      if (roles.contains("compute")) {
        val parallel = conf.parallelization.apply()
        val countOption = conf.count.toOption
        val props = ComputeActor.props(lifecycleActor, dao, parallel, countOption)
        system.actorOf(props, name = "compute")
      }
      if (roles.contains("manage")) {
        val workloadOption = conf.workload.toOption
        val props = ManageActor.props(lifecycleActor, new SclrService(), dao, workloadOption)
        system.actorOf(props, name = "manage")
      }
    }

    system
  }
}
