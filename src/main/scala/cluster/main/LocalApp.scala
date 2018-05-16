package cluster.main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import cluster.sclr.Messages.Workload
import cluster.sclr.http.JsonFormatters._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object LocalApp {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = Sclr.run(parallel = 4)
    val work = Workload("m5000", 2, 0.24, useLPNorm = true, optionalSubset = Some(10))
    val responseFuture = Marshal(work).to[RequestEntity] flatMap { entity =>
      println(s"Sending entity: $entity")
      val request = HttpRequest(method = HttpMethods.POST, uri = "http://127.0.0.1:8080/begin", entity = entity)
      Http().singleRequest(request)
    }

    Await.result(responseFuture, Duration.Inf)
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
//{"name":"m5000","dnfSize":2,"optionalSample":500,"useLPNorm":true,"mu":0.2465}
