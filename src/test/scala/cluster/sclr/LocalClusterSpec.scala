package cluster.sclr

import akka.actor.ActorSystem
import akka.cluster.pubsub.DistributedPubSubMediator.SubscribeAck
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.testkit.{ImplicitSender, TestKit}
import cluster.sclr.Messages.{Finished, Ready, topicComputer}
import cluster.sclr.actors.{ComputeActor, ManageActor}
import combinations.{CombinationAggregation, CombinationBuilder}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps

class LocalClusterSpec extends TestKit(ActorSystem("LocalClusterSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  val mediator = DistributedPubSub(system).mediator
  override def beforeAll: Unit = {
    ignoreMsg {case SubscribeAck(_) => true}
    mediator ! DistributedPubSubMediator.Subscribe(Messages.topicStatus, self)
  }

  override def afterAll: Unit = {
    mediator ! DistributedPubSubMediator.Unsubscribe(Messages.topicStatus, self)
    TestKit.shutdownActorSystem(system)
  }

  "The local cluster" must {

    "process work using a single compute node" in {
      val combinations = new CombinationAggregation(Vector(new CombinationBuilder(4,3), new CombinationBuilder(2,2)))
      val manageActor  = system.actorOf(ManageActor.props(combinations), "manage")
      val computeActor = system.actorOf(ComputeActor.props(), "compute")

      manageActor ! Ready

      expectMsg(1 seconds, Ready)
      expectMsg(5 seconds, Finished)


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
