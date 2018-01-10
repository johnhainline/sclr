package cluster.main

import akka.actor.{Actor, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages
import cluster.sclr.Messages.Finished
import cluster.sclr.actors.{ComputeActor, ManageActor}
import cluster.sclr.doobie.ResultsDao
import combinations.{CombinationAggregation, CombinationBuilder}

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class Terminator extends Actor {
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(Messages.topicStatus, self)
  def receive: Receive = {
    case Finished =>
      context.system.terminate()
  }
}

object LocalApp {

  def main(args: Array[String]): Unit = {

    val parallel = 3
    implicit val system: ActorSystem = ActorSystem("sclr")
    val joinAddress = Cluster(system).selfAddress
    Cluster(system).join(joinAddress)
    system.actorOf(Props(new Terminator()), "terminator")

    val resultsDao = new ResultsDao()
    val combinations = new CombinationAggregation(Vector(new CombinationBuilder(20,3), new CombinationBuilder(2,1)))
    val manageActor = system.actorOf(ManageActor.props(combinations), "manage")
    (1 to parallel).foreach(i => system.actorOf(ComputeActor.props(resultsDao)))

    manageActor ! Messages.Ready

    Await.result(system.whenTerminated, Duration.Inf)
  }
}
