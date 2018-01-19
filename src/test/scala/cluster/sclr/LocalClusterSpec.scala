//package cluster.sclr
//
//import akka.actor.ActorSystem
//import akka.cluster.pubsub.DistributedPubSubMediator.SubscribeAck
//import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
//import akka.pattern.gracefulStop
//import akka.testkit.{ImplicitSender, TestKit}
//import cluster.sclr.Messages.{Ack, Finished, Workload}
//import cluster.sclr.actors.{ComputeActor, ManageActor}
//import cluster.sclr.doobie.ResultsDao
//import org.scalamock.scalatest.MockFactory
//import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
//
//import scala.concurrent.Await
//import scala.concurrent.duration._
//import scala.language.postfixOps
//
//class LocalClusterSpec extends TestKit(ActorSystem("LocalClusterSpec")) with ImplicitSender
//  with WordSpecLike with Matchers with BeforeAndAfterAll with MockFactory {
//
//  val mediator = DistributedPubSub(system).mediator
//  override def beforeAll: Unit = {
//    ignoreMsg {case SubscribeAck(_) => true}
//    mediator ! DistributedPubSubMediator.Subscribe(Messages.topicStatus, self)
//  }
//
//  override def afterAll: Unit = {
//    mediator ! DistributedPubSubMediator.Unsubscribe(Messages.topicStatus, self)
//    TestKit.shutdownActorSystem(system)
//  }
//
//  "The local cluster" must {
//
//    "process work using a single compute node" in {
//      val resultsDao = mock[ResultsDao]
//      (resultsDao.setupDatabase _).expects().returning(1)
//      (resultsDao.setupTable _).expects("test", 1, 1, 2).returning(1)
//      (resultsDao.insertResult _).expects("test", true, Vector(0), Vector(0), Vector(0.0,1.0))
//      val manageActor  = system.actorOf(ManageActor.props(resultsDao), "manage")
//      val computeActor = system.actorOf(ComputeActor.props(resultsDao), "compute")
//      val workload = Workload("test", 1,1,1,1)
//
//      manageActor ! workload
//      expectMsg(1 seconds, Ack)
//
//      expectMsg(1 seconds, workload)
//      expectMsg(5 seconds, Finished)
//      Await.result(gracefulStop(computeActor, 5 seconds), Duration.Inf)
//      Await.result(gracefulStop(manageActor, 5 seconds), Duration.Inf)
//    }
//
//    "process work using 3 compute nodes" in {
//      val resultsDao = mock[ResultsDao]
////      (resultsDao.insertResult _).expects("Vector(Vector(0, 1, 2), Vector(0))")
////      (resultsDao.insertResult _).expects("Vector(Vector(0, 1, 2), Vector(1))")
////      (resultsDao.insertResult _).expects("Vector(Vector(0, 1, 3), Vector(0))")
////      (resultsDao.insertResult _).expects("Vector(Vector(0, 1, 3), Vector(1))")
////      (resultsDao.insertResult _).expects("Vector(Vector(0, 2, 3), Vector(0))")
////      (resultsDao.insertResult _).expects("Vector(Vector(0, 2, 3), Vector(1))")
////      (resultsDao.insertResult _).expects("Vector(Vector(1, 2, 3), Vector(0))")
////      (resultsDao.insertResult _).expects("Vector(Vector(1, 2, 3), Vector(1))")
//
//      val manageActor = system.actorOf(ManageActor.props(resultsDao), "manage")
//      val computeActors = (for (i <- 1 to 3) yield system.actorOf(ComputeActor.props(resultsDao))).toVector
//      val workload = Workload("test", 4, 3, 2, 1)
//
//      manageActor ! workload
//      expectMsg(1 seconds, Ack)
//
//      expectMsg(1 seconds, workload)
//      expectMsg(5 seconds, Finished)
//      for (actor <- computeActors) {
//        Await.result(gracefulStop(actor, 5 seconds), Duration.Inf)
//      }
//      Await.result(gracefulStop(manageActor, 5 seconds), Duration.Inf)
//    }
//
//  }
//}
