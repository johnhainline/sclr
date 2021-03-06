package sclr.core

import akka.actor.Kill
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import combinations.Combinations
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sclr.core.Messages._
import sclr.core.actors.{ComputeActor, LifecycleActor, ManageActor}
import sclr.core.database.DatabaseDao
import sclr.core.http.SclrService

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object ClusterSpecConfig extends MultiNodeConfig {
  // register the named roles (nodes) of the test
  val roleManage   = role(name="roleManage")
  val roleCompute1 = role(name="roleCompute1")
  val roleCompute2 = role(name="roleCompute2")
  val roleCompute3 = role(name="roleCompute3")

  def roleList = Seq(roleManage, roleCompute1, roleCompute2, roleCompute3)

  private def buildConfig(role: String, port: Int): Config = {
    ConfigFactory.parseString(
      s"""akka.cluster.roles=[$role]
         |akka.remote.netty.tcp.port=$port
         |akka.cluster.seed-nodes.0="akka.tcp://ClusterSpec@127.0.0.1:2551"
         |akka.stream.materializer.stream-ref.subscription-timeout=5 seconds""".stripMargin)
  }
  // this configuration will be used for all nodes
  commonConfig(ConfigFactory.defaultApplication())

  nodeConfig(roleManage)(buildConfig(role = "manage",    port = 2551))
  nodeConfig(roleCompute1)(buildConfig(role = "compute", port = 2552))
  nodeConfig(roleCompute2)(buildConfig(role = "compute", port = 2553))
  nodeConfig(roleCompute3)(buildConfig(role = "compute", port = 2554))
}

// need one concrete test class per node
class ClusterSpecMultiJvmNode1 extends ClusterSpec
class ClusterSpecMultiJvmNode2 extends ClusterSpec
class ClusterSpecMultiJvmNode3 extends ClusterSpec
class ClusterSpecMultiJvmNode4 extends ClusterSpec

abstract class ClusterSpec extends MultiNodeSpec(ClusterSpecConfig)
  with WordSpecLike with Matchers with BeforeAndAfterAll with Eventually {

  import ClusterSpecConfig._

  override def initialParticipants: Int = roleList.size

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()
  override def afterAll(): Unit = multiNodeSpecAfterAll()
  // Might not be needed anymore if we find a nice way to tag all logging from a node
  override implicit def convertToWordSpecStringWrapper(s: String): WordSpecStringWrapper = new WordSpecStringWrapper(s"$s (on node '${this.myself.name}', $getClass)")

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val work = Workload("tiny", 2, 0.24, useLPNorm = true)

  "The cluster" must {
    "run a test dataset" in within(max = 3 minutes) {
      runOn(roleManage) {
        system.actorOf(LifecycleActor.props(shutdown = true), name = "lifecycle")
        system.actorOf(ManageActor.props(new SclrService(), new DatabaseDao()), name = "manage")
      }

      runOn(roleCompute1, roleCompute2, roleCompute3) {
        system.actorOf(LifecycleActor.props(shutdown = true), name = "lifecycle")
        system.actorOf(ComputeActor.props(new DatabaseDao(), parallel = 1), name = "compute")
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
        val run = new Runnable {
          override def run(): Unit = {
            implicit val ec = system.dispatcher
            Thread.sleep(2000)
            system.actorSelection("/user/compute") ! Kill
            system.scheduler.scheduleOnce(2 seconds, new Runnable {
              override def run(): Unit = {
                Thread.sleep(1000)
                system.actorOf(ComputeActor.props(new DatabaseDao(), parallel = 1), name = "compute")
              }
            })
          }
        }
        implicit val ec = system.dispatcher
//        system.scheduler.scheduleOnce(4 seconds, run)
//        system.scheduler.scheduleOnce(7 seconds, run)
        system.scheduler.schedule(4 seconds, 8 seconds, run)
      }

      enterBarrier(name = "killed")

      runOn(roleCompute1) {
        val dbDao = new DatabaseDao()
        val finalCount = Combinations(6, 2).size.toLong * Combinations(50, 2).size.toLong
        val xa = DatabaseDao.makeHikariTransactor()
        eventually(timeout(2 minutes), interval(3 seconds)) {
          val results = dbDao.getResultCount(xa, work.name)
          println(results)
          results should be(finalCount)
        }
      }

      enterBarrier(name = "finished")
    }
  }
}
