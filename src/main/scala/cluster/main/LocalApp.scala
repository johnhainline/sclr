package cluster.main

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import cluster.sclr.Messages
import cluster.sclr.actors.{ComputeActor, ManageActor}
import cluster.sclr.doobie.ResultsDao

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object LocalApp {

  def main(args: Array[String]): Unit = {

    val parallel = 3
    implicit val system: ActorSystem = ActorSystem("sclr")
    val joinAddress = Cluster(system).selfAddress
    Cluster(system).join(joinAddress)
    system.actorOf(Props(new Terminator()), "terminator")

    val resultsDao = new ResultsDao()
    val manageActor = system.actorOf(ManageActor.props(resultsDao), "manage")
    (1 to parallel).foreach(i => system.actorOf(ComputeActor.props(resultsDao)))

    manageActor ! Messages.Workload("house", 5, 2, 7, 3)

    Await.result(system.whenTerminated, Duration.Inf)
  }
}
