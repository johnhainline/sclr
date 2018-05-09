package cluster.main

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.stream.ActorMaterializer
import cluster.sclr.Messages.Workload
import cluster.sclr.actors.{ComputeActor, FrontendActor, ManageActor}
import cluster.sclr.core.DatabaseDao
import cluster.sclr.http.InfoService
import cluster.sclr.http.JsonFormatters._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object LocalApp {


  def main(args: Array[String]): Unit = {

    val parallel = 1
    implicit val system: ActorSystem = ActorSystem("sclr")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val joinAddress = Cluster(system).selfAddress
    Cluster(system).join(joinAddress)

    val dao = new DatabaseDao()
    val infoService = new InfoService()
    system.actorOf(ManageActor.props(dao), name = "manage")
    system.actorOf(ComputeActor.props(parallel, dao), name = "compute")
    system.actorOf(FrontendActor.props(infoService), name = "frontend")

    val work = Workload("tiny", 2, 0.24, useLPNorm = true)
    val responseFuture = Marshal(work).to[RequestEntity] flatMap { entity =>
      println(s"Sending entity: $entity")
      val request = HttpRequest(method = HttpMethods.POST, uri = "http://127.0.0.1:8080/begin", entity = entity)
      Http().singleRequest(request)
    }

    Await.result(responseFuture, Duration.Inf)
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
