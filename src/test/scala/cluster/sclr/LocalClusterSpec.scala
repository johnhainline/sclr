package cluster.sclr

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSubMediator.SubscribeAck
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.testkit.{ImplicitSender, TestKit}
import cluster.sclr.Messages._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps
import scala.concurrent.duration._

class LocalClusterSpec extends TestKit(ActorSystem("LocalClusterSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  val mediator = DistributedPubSub(system).mediator

  override def beforeAll: Unit = {
    ignoreMsg {case SubscribeAck(_) => true}
    mediator ! DistributedPubSubMediator.Subscribe(topicProcessingComplete, self)
  }

  override def afterAll: Unit = {
    mediator ! DistributedPubSubMediator.Unsubscribe(topicProcessingComplete, self)
    TestKit.shutdownActorSystem(system)
  }

  "The local cluster" must {

    "process work for 1 node" in {
      val joinAddress = Cluster(system).selfAddress
      Cluster(system).join(joinAddress)

      system.actorOf(GiveWorkActor.props(), "giveWork")
      system.actorOf(DoWorkActor.props(), s"doWork")
      val saveResultActor = system.actorOf(SaveResultActor.props(), "saveResult")

      // Wait for actors to connect via pub/sub
      Thread.sleep(1000)

      saveResultActor ! SaveResultActor.AskForResult

      expectMsg(5 seconds, ProcessingComplete)
    }

  }
}