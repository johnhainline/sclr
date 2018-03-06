package cluster.main

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.stream.ActorMaterializer
import cluster.sclr.Messages.Workload
import cluster.sclr.actors.{ComputeActor, FrontendActor, ManageActor}
import cluster.sclr.core.DatabaseDao
import cluster.sclr.http.InfoService

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import cluster.sclr.http.JsonFormatters._

object LocalApp {


  def main(args: Array[String]): Unit = {

    val parallel = 4
    implicit val system: ActorSystem = ActorSystem("sclr")
    implicit val materializer: ActorMaterializer = ActorMgaterializer()
    val joinAddress = Cluster(system).selfAddress
    Cluster(system).join(joinAddress)
    system.actorOf(Props(new Terminator()), "terminator")

    val resultsDao = new DatabaseDao()
    system.actorOf(ManageActor.props(resultsDao), "manage")
    (1 to parallel).foreach(_ => system.actorOf(ComputeActor.props(resultsDao)))
    system.actorOf(FrontendActor.props(new InfoService()), "frontend")

    val work = Workload("m1000", 2, 0.24)
    val responseFuture = Marshal(work).to[RequestEntity] flatMap { entity =>
      println(s"Sending entity: $entity")
      val request = HttpRequest(method = HttpMethods.POST, uri = "http://127.0.0.1:8080/begin", entity = entity)
      Http().singleRequest(request)
    }

    Await.result(responseFuture, Duration.Inf)
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
