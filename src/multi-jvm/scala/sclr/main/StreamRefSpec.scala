package sclr.main

import akka.actor.Kill
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.stream.ActorMaterializer
import sclr.core.Messages.Workload
import sclr.core.actors.{ComputeActor, LifecycleActor, ManageActor}
import sclr.core.database.DatabaseDao
import sclr.core.http.SclrService
import com.typesafe.config.{Config, ConfigFactory}
import combinations.Combinations
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatest.concurrent.Eventually

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object StreamRefSpecConfig extends MultiNodeConfig {
  // register the named roles (nodes) of the test
  val roleManage = role(name="role1")
  val roleCompute1 = role(name="role2")
  val roleCompute2 = role(name="role3")
  val roleCompute3 = role(name="role4")

  def roleList = Seq(roleManage, roleCompute1, roleCompute2, roleCompute3)

  private def buildConfig(role: String, port: Int): Config = {
    ConfigFactory.parseString(
      s"""akka.cluster.roles=[$role]
         |akka.remote.netty.tcp.port=$port
         |akka.cluster.seed-nodes.0="akka.tcp://StreamRefSpec@127.0.0.1:2551"
         |akka.stream.materializer.stream-ref.subscription-timeout=5 seconds""".stripMargin)
  }
  // this configuration will be used for all nodes
  commonConfig(ConfigFactory.defaultApplication())

  nodeConfig(roleManage)(buildConfig(role = "role1", port = 2551))
  nodeConfig(roleCompute1)(buildConfig(role = "role2", port = 2552))
  nodeConfig(roleCompute2)(buildConfig(role = "role3", port = 2553))
  nodeConfig(roleCompute3)(buildConfig(role = "role4", port = 2554))
}

// need one concrete test class per node
class StreamRefSpecMultiJvmNode1 extends StreamRefSpec
class StreamRefSpecMultiJvmNode2 extends StreamRefSpec
class StreamRefSpecMultiJvmNode3 extends StreamRefSpec
class StreamRefSpecMultiJvmNode4 extends StreamRefSpec

abstract class StreamRefSpec extends MultiNodeSpec(StreamRefSpecConfig)
  with WordSpecLike with Matchers with BeforeAndAfterAll with Eventually {

  import StreamRefSpecConfig._

  override def initialParticipants: Int = roleList.size

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()
  override def afterAll(): Unit = multiNodeSpecAfterAll()
  // Might not be needed anymore if we find a nice way to tag all logging from a node
  override implicit def convertToWordSpecStringWrapper(s: String): WordSpecStringWrapper = new WordSpecStringWrapper(s"$s (on node '${this.myself.name}', $getClass)")

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val work = Workload("tiny", 2, 0.24, useLPNorm = true)

  "The cluster" must {
    "handle basic connections between StreamRefs" in within(max = 1 minutes) {
      runOn(roleManage) {
        val lifecycle = system.actorOf(LifecycleActor.props(), name = "lifecycle")
        system.actorOf(ManageActor.props(lifecycle, new SclrService(), new DatabaseDao(), None), name = "manage")
      }

      runOn(roleCompute1, roleCompute2, roleCompute3) {
        val lifecycle = system.actorOf(LifecycleActor.props(), name = "lifecycle")
        system.actorOf(ComputeActor.props(lifecycle, new DatabaseDao(), parallel = 1), name = "compute")
      }

      enterBarrier(name = "initialized")

      runOn(roleCompute1) {
        import sclr.core.http.SclrService._
        import system.dispatcher
        val responseFuture = Marshal(work).to[RequestEntity] flatMap { entity =>
          println(s"Sending entity: $entity")
          val request = HttpRequest(method = HttpMethods.POST, uri = "http://127.0.0.1:8080/begin", entity = entity)
          Http().singleRequest(request)
        }
        Await.result(responseFuture, Duration.Inf)
      }

      enterBarrier(name = "started")

      runOn(roleCompute1, roleCompute2, roleCompute3) {
        Thread.sleep(2000)
        system.actorSelection("/user/compute") ! Kill
        Thread.sleep(4000)
        val lifecycle = Await.result(system.actorSelection(path = "user/lifecycle").resolveOne(5 seconds), 5 seconds)
        system.actorOf(ComputeActor.props(lifecycle, new DatabaseDao(), parallel = 1), name = "compute")
      }

      enterBarrier(name = "killed")

      runOn(roleCompute1) {
        val dbDao = new DatabaseDao()
        val finalCount = Combinations(6, 2).size.toLong * Combinations(50, 2).size.toLong
        val xa = DatabaseDao.makeHikariTransactor()
        eventually (timeout(2 minutes), interval(3 seconds)) {
          val results = dbDao.getResultCount(xa, work.name)
          println(results)
          results should be (finalCount)
        }
      }

      enterBarrier(name = "finished")
    }


    "manage StreamRef downing" in within(max = 3 minutes) {
      runOn(roleManage) {
        val lifecycle = system.actorOf(LifecycleActor.props(), name = "lifecycle")
        system.actorOf(ManageActor.props(lifecycle, new SclrService(), new DatabaseDao(), None), name = "manage")
      }

      runOn(roleCompute1, roleCompute2, roleCompute3) {
        val lifecycle = system.actorOf(LifecycleActor.props(), name = "lifecycle")
        system.actorOf(ComputeActor.props(lifecycle, new DatabaseDao(), parallel = 1), name = "compute")
      }

      enterBarrier(name = "initialized")

      runOn(roleCompute1) {
        import sclr.core.http.SclrService._
        import system.dispatcher
        val responseFuture = Marshal(work).to[RequestEntity] flatMap { entity =>
          println(s"Sending entity: $entity")
          val request = HttpRequest(method = HttpMethods.POST, uri = "http://127.0.0.1:8080/begin", entity = entity)
          Http().singleRequest(request)
        }
        Await.result(responseFuture, Duration.Inf)
      }

      enterBarrier(name = "started")

      runOn(roleCompute1, roleCompute2, roleCompute3) {
        Thread.sleep(2000)
        system.actorSelection("/user/compute") ! Kill
        Thread.sleep(4000)
        val lifecycle = Await.result(system.actorSelection(path = "user/lifecycle").resolveOne(5 seconds), 5 seconds)
        system.actorOf(ComputeActor.props(lifecycle, new DatabaseDao(), parallel = 1), name = "compute")
      }

      enterBarrier(name = "killed")

      runOn(roleCompute1) {
        val dbDao = new DatabaseDao()
        val finalCount = Combinations(6, 2).size.toLong * Combinations(50, 2).size.toLong
        val xa = DatabaseDao.makeHikariTransactor()
        eventually (timeout(2 minutes), interval(3 seconds)) {
          val results = dbDao.getResultCount(xa, work.name)
          println(results)
          results should be (finalCount)
        }
      }

      enterBarrier(name = "finished")
    }
  }
}
