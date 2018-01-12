//package cluster.sclr.http
//
//import akka.actor.{ActorRef, ActorSystem}
//import akka.cluster.Member
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
//import akka.http.scaladsl.server.Directives
//import akka.pattern.ask
//import akka.stream.ActorMaterializer
//import cluster.sclr.actors.FrontendActor
//import cluster.sclr.actors.FrontendActor.MembershipInfo
//import spray.json.DefaultJsonProtocol
//
//class InfoService(frontendActor: ActorRef)(implicit val system: ActorSystem, implicit val materializer: ActorMaterializer)
//  extends Directives with SprayJsonSupport with DefaultJsonProtocol {
//
//  implicit val memberFormat = jsonFormat1(Member)
//  implicit val membershipInfoFormat = jsonFormat1(MembershipInfo)
//  implicit val executionContext = system.dispatcher
//
//  val route =
//    path("info") {
//      get {
//        onSuccess((frontendActor ? FrontendActor.GetMembershipInfo).mapTo[FrontendActor.MembershipInfo]) { result =>
//          complete(result)
//        }
//      }
//    }
//
//  Http().bindAndHandle(route, "localhost", 8080)
//}
