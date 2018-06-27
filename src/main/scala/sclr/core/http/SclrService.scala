package sclr.core.http

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.pattern.ask
import akka.remote.Ack
import akka.stream.ActorMaterializer
import akka.util.Timeout
import sclr.core.Messages.Workload
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import spray.json.{DefaultJsonProtocol, PrettyPrinter}

import scala.concurrent.duration._
import scala.language.postfixOps

object SclrService extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val workloadFormat = jsonFormat7(Workload)
}

class SclrService(implicit val system: ActorSystem, implicit val materializer: ActorMaterializer)
  extends Directives with LazyLogging {

  import SclrService._

  var manageActorOption: Option[ActorRef] = None

  implicit val printer = PrettyPrinter
  implicit val timeout = Timeout(30 seconds)

  implicit val executionContext = system.dispatcher

  val route =
    path("begin") {
      post {
        decodeRequest {
          entity(as[Workload]) { workload =>
            manageActorOption match {
              case Some(manageActor) =>
                onSuccess(manageActor ? workload) {
                  case Ack =>
                    complete(StatusCodes.OK)
                  case _ =>
                    complete(StatusCodes.InternalServerError)
                }
              case None =>
                complete {
                  HttpResponse(StatusCodes.FailedDependency,
                    entity = HttpEntity(
                      ContentType(MediaTypes.`application/json`),
                      """{"msg":"ManagerActor reference has not yet been acquired."}"""
                    )
                  )
                }
            }
          }
        }
      }
    }

  val host = ConfigFactory.load().getString("akka.http.server.default-http-host")
  logger.debug(s"Binding to host: $host")
  Http().bindAndHandle(route, host)

  def setManageActor(manageActor:ActorRef) = {
    manageActorOption = Some(manageActor)
  }
}
