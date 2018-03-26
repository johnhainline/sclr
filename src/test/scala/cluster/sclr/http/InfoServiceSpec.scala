package cluster.sclr.http

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.testkit.{TestActor, TestProbe}
import cluster.sclr.Messages.{Workload, Ack}
import cluster.sclr.http.InfoService
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
      Get("/begin") ~> infoService.route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/kermit") ~> infoService.route ~> check {
        handled shouldBe false
      }
    }
  }
}
