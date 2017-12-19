package cluster.sclr

import akka.actor.ActorSystem
import akka.cluster.Cluster

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object LocalDeploy {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("sclr")

    val joinAddress = Cluster(system).selfAddress
    Cluster(system).join(joinAddress)

    val giveWorkActor = system.actorOf(GiveWorkActor.props())
    val doWorkActors = (0 to 10).foreach(_ => system.actorOf(DoWorkActor.props()))
    val saveResultActor = system.actorOf(SaveResultActor.props())


    Thread.sleep(1000)

    saveResultActor ! SaveResultActor.AskForResult

    Await.result(system.whenTerminated, Duration.Inf)
  }
}
