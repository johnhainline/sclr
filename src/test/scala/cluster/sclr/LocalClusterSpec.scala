package cluster.sclr

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class LocalClusterSpec extends TestKit(ActorSystem("LocalClusterSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
  val parallel = 10

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

      Await.result(system.whenTerminated, Duration.Inf)

      val echo = system.actorOf(TestActors.echoActorProps)
      echo ! "hello world"
      expectMsg("hello world")
    }

  }
}