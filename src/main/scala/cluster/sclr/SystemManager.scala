package cluster.sclr

import akka.Done
import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import cluster.sclr.actors.ManageActor
import combinations.CombinationAggregation

class SystemManager(combinations: CombinationAggregation) {
  implicit val system = ActorSystem("sclr")

  val joinAddress = Cluster(system).selfAddress
  Cluster(system).join(joinAddress)

  val singletonManager = system.actorOf(
    ClusterSingletonManager.props(
      singletonProps = ManageActor.props(combinations),
      terminationMessage = Done,
      settings = ClusterSingletonManagerSettings(system).withRole("manager")),
    name = "consumer")

//  import akka.http.scaladsl.Http
//  import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
//  import akka.stream.ActorMaterializer
//  import akka.http.scaladsl.model._
//  import akka.http.scaladsl.server.Directives._
//  implicit val materializer = ActorMaterializer()
//  implicit val executionContext = system.dispatcher
//  val route =
//    path("info") {
//      get {
//        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
//        clusterMembership.ask(ClusterMembership.GetMembershipInfo)(askTimeout).mapTo[ClusterMembership.MembershipInfo]
//      }
//    }
//
//  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
//
//  bindingFuture
//    .flatMap(_.unbind()) // trigger unbinding from the port
//    .onComplete(_ => system.terminate()) // and shutdown when done


}
