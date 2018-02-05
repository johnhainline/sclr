package cluster.sclr.http

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import cluster.sclr.Messages.{Ack, Workload}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import combinations.{CombinationAggregation, CombinationBuilder}
import spray.json.{DefaultJsonProtocol, JsArray, JsNumber, JsObject, JsValue, JsonFormat, PrettyPrinter}

import scala.concurrent.duration._
import scala.language.postfixOps

class InfoService(manageActor: ActorRef)(implicit val system: ActorSystem, implicit val materializer: ActorMaterializer)
  extends Directives with LazyLogging {

  implicit val printer = PrettyPrinter
  implicit val timeout = Timeout(30 seconds)

  implicit val executionContext = system.dispatcher

  import cluster.sclr.http.JsonFormatters._

  val route =
    path("begin") {
      post {
        decodeRequest {
          entity(as[Workload]) { workload =>
            onSuccess(manageActor ? workload) {
              case Ack =>
                complete(StatusCodes.OK)
              case _ =>
                complete(StatusCodes.InternalServerError)
            }
          }
        }
      }
    }

  val host = ConfigFactory.load().getString("akka.http.server.default-http-host")
  logger.debug(s"Binding to host: $host")
  Http().bindAndHandle(route, host)
}

object JsonFormatters extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val workloadFormat = jsonFormat7(Workload)

  implicit val combinationBuilderFormat = new JsonFormat[CombinationBuilder] {
    def write(x: CombinationBuilder) = JsObject("n" -> JsNumber(x.n), "k" -> JsNumber(x.k))

    def read(value: JsValue) = {
      val jsObject = value.asJsObject
      jsObject.getFields("n", "k") match {
        case Seq(n, k) =>
          CombinationBuilder(n.convertTo[Int], k.convertTo[Int])
        case x =>
          throw new RuntimeException(s"Unexpected type %s on parsing of CombinationBuilder type".format(x.getClass.getName))
      }
    }
  }

  implicit val combinationAggregationFormat = new JsonFormat[CombinationAggregation] {
    def write(x: CombinationAggregation) = {
      JsObject("combinations" -> JsArray(x.combinations.map(combinationBuilderFormat.write)))
    }

    def read(value: JsValue) = {
      val jsObject = value.asJsObject
      jsObject.getFields("combinations") match {
        case Seq(combinations:JsArray) =>
          CombinationAggregation(combinations.elements.map(combinationBuilderFormat.read))
        case x =>
          throw new RuntimeException(s"Unexpected type %s on parsing of CombinationAggregation type".format(x.getClass.getName))
      }
    }
  }
}