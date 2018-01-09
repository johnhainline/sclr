package cluster.main

import akka.actor.ActorSystem
import akka.cluster.Cluster
import cluster.sclr.Messages
import cluster.sclr.actors.{ComputeActor, ManageActor}
import combinations.{CombinationAggregation, CombinationBuilder}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object LocalApp {

  def main(args: Array[String]): Unit = {

    val parallel = 10
    implicit val system = ActorSystem("sclr")

    val joinAddress = Cluster(system).selfAddress
    Cluster(system).join(joinAddress)

    val combinations = new CombinationAggregation(Vector(new CombinationBuilder(4,3), new CombinationBuilder(2,2)))
    val manageActor = system.actorOf(ManageActor.props(combinations), "manage")
    (1 to parallel).foreach(i => system.actorOf(ComputeActor.props()))

    manageActor ! Messages.Ready

    Await.result(system.whenTerminated, Duration.Inf)
  }
}
