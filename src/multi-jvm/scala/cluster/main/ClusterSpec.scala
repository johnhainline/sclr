package cluster.sclr

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.stream.ActorMaterializer
import cluster.sclr.Messages._
import cluster.sclr.actors.{ComputeActor, ManageActor}
import cluster.sclr.core.DatabaseDao
import cluster.sclr.http.InfoService
import com.typesafe.config.{Config, ConfigFactory}
import combinations.Combinations
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

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

  "The cluster" must {
    "run a test dataset" in within(max = 5 minutes) {
      runOn(roleManage) {
        system.actorOf(ManageActor.props(new InfoService(), new DatabaseDao()), name = "manage")
      }

      runOn(roleCompute1, roleCompute2, roleCompute3) {
        system.actorOf(ComputeActor.props(parallelization = 1, new DatabaseDao()), name = "compute")
      }

      enterBarrier(name = "initialized")

      import cluster.sclr.http.JsonFormatters._
      import system.dispatcher
      val work = Workload("tiny", 2, 0.24, useLPNorm = true)
      val responseFuture = Marshal(work).to[RequestEntity] flatMap { entity =>
        println(s"Sending entity: $entity")
        val request = HttpRequest(method = HttpMethods.POST, uri = "http://127.0.0.1:8080/begin", entity = entity)
        Http().singleRequest(request)
      }
      Await.result(responseFuture, Duration.Inf)
      val dbDao = new DatabaseDao()
      val finalCount = Combinations(6, 2).size.toLong * Combinations(50, 2).size.toLong
      eventually (timeout(120 seconds), interval(3 seconds)) {
        val results = dbDao.getResultCount(work.name)
        println(results)
        results should be (finalCount)
      }

      enterBarrier(name = "finished")
    }
  }
}
