package cluster.sclr

import java.sql.Connection

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.testkit.{ImplicitSender, TestKit}
import cluster.sclr.Messages.Complete
import cluster.sclr.actors.{ComputeActor, ManageActor, SaveActor}
import combinations.{CombinationAggregation, CombinationBuilder}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps

class LocalClusterSpec extends TestKit(ActorSystem("LocalClusterSpec")) with ImplicitSender
  with WordSpecLike with MockFactory with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "The local cluster" must {

    "process work using a single compute node" in {
      val stubConnection = stub[Connection]
      val joinAddress = Cluster(system).selfAddress
      Cluster(system).join(joinAddress)

      val combinations = new CombinationAggregation(Vector(new CombinationBuilder(4,3), new CombinationBuilder(2,2)))
      system.actorOf(ManageActor.props(combinations), "manage")
      system.actorOf(ComputeActor.props(), "compute")
      val saveResultActor = system.actorOf(SaveActor.props(() => stubConnection), "save")

      // Wait for actors to connect via pub/sub
      Thread.sleep(1000)

      //      saveResultActor ! SaveActor.AskForResult

      expectMsg(5 seconds, Complete)
    }

//    "process work using 10 compute nodes" in {
//      val stubConnection = stub[Connection]
//      val joinAddress = Cluster(system).selfAddress
//      Cluster(system).join(joinAddress)
//
//      val combinations = new CombinationAggregation(Vector(new CombinationBuilder(4,3), new CombinationBuilder(2,2)))
//      system.actorOf(ManageActor.props(combinations), "manage")
//      (1 to 10).foreach(i => system.actorOf(ComputeActor.props()))
//      val saveResultActor = system.actorOf(SaveActor.props(() => stubConnection), "save")
//
//      // Wait for actors to connect...
//      Thread.sleep(1000)
//
//      //      saveResultActor ! SaveActor.AskForResult
//
//      expectMsg(5 seconds, Complete)
//    }

  }
}
