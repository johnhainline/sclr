package cluster.main

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import cluster.sclr.Messages
import cluster.sclr.actors.{ComputeActor, ManageActor}
import cluster.sclr.doobie.ResultsDao
import combinations.{CombinationAggregation, CombinationBuilder}

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
    resultsDao.setupDatabase()
    val manageActor = system.actorOf(ManageActor.props(), "manage")
    (1 to parallel).foreach(i => system.actorOf(ComputeActor.props(resultsDao)))

    val combinations = new CombinationAggregation(Vector(new CombinationBuilder(5,2), new CombinationBuilder(7,3)))
    manageActor ! Messages.Begin(combinations, "house.csv")

    Await.result(system.whenTerminated, Duration.Inf)
  }
}
