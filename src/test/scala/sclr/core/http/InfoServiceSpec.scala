package sclr.core.http

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.remote.Ack
import akka.stream.ActorMaterializer
import akka.testkit.{TestActor, TestProbe}
import akka.util.ByteString
import sclr.core.Messages.Workload
import org.scalatest.{Matchers, WordSpec}

class InfoServiceSpec extends WordSpec with ScalatestRouteTest with Matchers {

  val probe = TestProbe()
  val infoService = new InfoService()(system, ActorMaterializer())
  infoService.setManageActor(probe.ref)

  "The service" should {

    "return a response for GET requests to /begin" in {
      probe.setAutoPilot((sender: ActorRef, msg: Any) => {
        msg match {
          case _: Workload =>
            sender ! Ack
        }
        TestActor.KeepRunning
      })
      val jsonRequest = ByteString(s"""{"name":"example","dnfSize":2, "mu":0.24, "useLPNorm":true}""".stripMargin)
      val postRequest = HttpRequest(HttpMethods.POST,uri = "/begin",entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))
      postRequest ~> infoService.route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
    }

    "leave GET requests to other paths unhandled" in {
      Post("/kermit") ~> infoService.route ~> check {
        handled shouldBe false
      }
    }
  }
}
