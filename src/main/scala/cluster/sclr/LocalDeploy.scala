package cluster.sclr

import akka.actor.ActorSystem
import akka.cluster.Cluster

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object LocalDeploy {
  def main(args: Array[String]): Unit = {

    val parallel = 10
    implicit val system = ActorSystem("sclr")

    val joinAddress = Cluster(system).selfAddress
    Cluster(system).join(joinAddress)

    system.actorOf(GiveWorkActor.props(), "giveWork")
    (1 to parallel).foreach(i => system.actorOf(DoWorkActor.props(), s"doWork$i"))
    val saveResultActor = system.actorOf(SaveResultActor.props(), "saveResult")


    Thread.sleep(1000)

    (1 to parallel).foreach(_ =>
      saveResultActor ! SaveResultActor.AskForResult
    )

    Await.result(system.whenTerminated, Duration.Inf)
  }
}
