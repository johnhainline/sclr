package sclr.main

import akka.actor.{ActorSystem, Kill}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import sclr.core.Messages.Workload
import sclr.core.actors.ComputeActor
import sclr.core.database.DatabaseDao
import sclr.core.http.InfoService._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object LocalApp {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = Sclr.run(parallel = 4)
    val work = Workload("rsv", 2, 0.2, useLPNorm = true)
    val responseFuture = Marshal(work).to[RequestEntity] flatMap { entity =>
      println(s"Sending entity: $entity")
      val request = HttpRequest(method = HttpMethods.POST, uri = "http://127.0.0.1:8080/begin", entity = entity)
      Http().singleRequest(request)
    }

//    val sleep = 3000
//    Thread.sleep(sleep)
//    system.actorSelection("/user/computeSupervisor") ! Kill
//    Thread.sleep(sleep)
//    Sclr.runResumeSupervisorForActor(name = "compute", ComputeActor.props(1, new DatabaseDao()))
//    Thread.sleep(sleep)
//    system.actorSelection("/user/computeSupervisor") ! Kill
//    Thread.sleep(sleep)
//    Sclr.runResumeSupervisorForActor(name = "compute", ComputeActor.props(4, new DatabaseDao()))

    Await.result(responseFuture, Duration.Inf)
  }
}
