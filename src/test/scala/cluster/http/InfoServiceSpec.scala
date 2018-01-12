//package cluster.http
//
//import akka.actor.ActorSystem
//import akka.http.scaladsl.model.StatusCodes
//import akka.http.scaladsl.server.Route
//import akka.http.scaladsl.testkit.ScalatestRouteTest
//import akka.stream.ActorMaterializer
//import akka.testkit.{ImplicitSender, TestKit, TestProbe}
//import cluster.sclr.actors.FrontendActor.{GetMembershipInfo, MembershipInfo}
//import cluster.sclr.http.InfoService
//import org.scalatest.{Matchers, WordSpecLike}
//
//class InfoServiceSpec extends TestKit(ActorSystem("InfoServiceSpec")) with ImplicitSender
//with WordSpecLike with Matchers with ScalatestRouteTest {
//
//  val probe = TestProbe()
//  val infoService = new InfoService(probe.ref)(system, ActorMaterializer())
//
//  "The service" should {
//
//    "return a response for GET requests to /info" in {
//      val response = MembershipInfo(Set.empty)
//      probe.reply {
//        case GetMembershipInfo =>
//          response
//      }
//      Get("/info") ~> infoService.route ~> check {
//        responseAs[MembershipInfo] shouldEqual response
//      }
//    }
//
//    "leave GET requests to other paths unhandled" in {
//      Get("/kermit") ~> infoService.route ~> check {
//        handled shouldBe false
//      }
//    }
//
//    "return a MethodNotAllowed error for PUT requests to the root path" in {
//      Put() ~> Route.seal(infoService.route) ~> check {
//        status shouldEqual StatusCodes.MethodNotAllowed
//        responseAs[String] shouldEqual "HTTP method not allowed, supported methods: GET"
//      }
//    }
//  }
//}
