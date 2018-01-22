package cluster.main

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import cluster.sclr.actors.{ComputeActor, FrontendActor, ManageActor}
import cluster.sclr.doobie.ResultsDao
import cluster.sclr.http.InfoService

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object LocalApp {

  def main(args: Array[String]): Unit = {

    val parallel = 3
    implicit val system: ActorSystem = ActorSystem("sclr")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val joinAddress = Cluster(system).selfAddress
    Cluster(system).join(joinAddress)
    system.actorOf(Props(new Terminator()), "terminator")

    val resultsDao = new ResultsDao()
    val manageActor = system.actorOf(ManageActor.props(resultsDao), "manage")
    (1 to parallel).foreach(_ => system.actorOf(ComputeActor.props(resultsDao)))
    val infoService = new InfoService(manageActor)
    system.actorOf(FrontendActor.props(infoService), "frontend")

    val responseFuture = Http().singleRequest(HttpRequest(uri = "http://127.0.0.1:8080/begin"))

    Await.result(responseFuture, Duration.Inf)
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
