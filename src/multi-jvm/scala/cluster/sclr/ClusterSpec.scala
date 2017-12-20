package cluster.sclr

import akka.actor.DeadLetter
import akka.cluster.Cluster
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import cluster.sclr.SaveResultActor.AskForResult
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps

object ClusterSpecConfig extends MultiNodeConfig {
  // register the named roles (nodes) of the test
  val roleGiveWork = role("roleGiveWork")
  val roleDoWork1 = role("roleDoWork1")
  val roleDoWork2 = role("roleDoWork2")
  val roleDoWork3 = role("roleDoWork3")
  val roleSaveResults = role("roleSaveResults")

  def nodeList = Seq(roleGiveWork, roleDoWork1, roleDoWork2, roleDoWork3, roleSaveResults)

  // Extract individual sigar library for every node.
  nodeList foreach { role =>
    nodeConfig(role) {
      ConfigFactory.parseString(s"""
      # Sigar native library extract location during tests.
      akka.cluster.metrics.native-library-extract-folder=target/native/${role.name}
      """)
    }
  }

  // this configuration will be used for all nodes
  // note that no fixed host names and ports are used
  commonConfig(ConfigFactory.parseString("""
    akka.actor.provider = cluster
    akka.remote.log-remote-lifecycle-events = off
    """))

  nodeConfig(roleGiveWork)(
    ConfigFactory.parseString("akka.cluster.roles =[giveWork]"))

  nodeConfig(roleDoWork1, roleDoWork2, roleDoWork3)(
    ConfigFactory.parseString("akka.cluster.roles =[doWork]"))

  nodeConfig(roleSaveResults)(
    ConfigFactory.parseString("akka.cluster.roles =[saveResults]"))
}

// need one concrete test class per node
class ClusterSpecMultiJvmNode1 extends ClusterSpec
class ClusterSpecMultiJvmNode2 extends ClusterSpec
class ClusterSpecMultiJvmNode3 extends ClusterSpec
class ClusterSpecMultiJvmNode4 extends ClusterSpec
class ClusterSpecMultiJvmNode5 extends ClusterSpec

abstract class ClusterSpec extends MultiNodeSpec(ClusterSpecConfig)
  with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  import ClusterSpecConfig._

  override def initialParticipants = roles.size

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()

  "The cluster" must {
    "start correctly" in within(15 seconds) {
      runOn(roleGiveWork) {
        // this will only run on the 'first' node
        Cluster(system) join node(roleGiveWork).address
        val actor = system.actorOf(SaveResultActor.props(), "saveResult")
        //subscribe to system wide event bus 'DeadLetter'
        system.eventStream.subscribe(self, classOf[DeadLetter])
        actor ! AskForResult
        expectMsg(DeadLetter)
      }

      // this will run on all nodes
      // use barrier to coordinate test steps
      testConductor.enter("frontend1-started")
    }

//    "automatically register new doWork actors" in within(15 seconds) {
//      runOn(backend1) {
//        Cluster(system) join node(frontend1).address
//        system.actorOf(Props[TransformationBackend], name = "backend")
//      }
//      testConductor.enter("backend1-started")
//
//      runOn(frontend1) {
//        assertServiceOk()
//      }
//
//      testConductor.enter("frontend1-backend1-ok")
//    }
//
//    "illustrate how more nodes registers" in within(20 seconds) {
//      runOn(frontend2) {
//        Cluster(system) join node(frontend1).address
//        system.actorOf(Props[TransformationFrontend], name = "frontend")
//      }
//      testConductor.enter("frontend2-started")
//
//      runOn(backend2, backend3) {
//        Cluster(system) join node(backend1).address
//        system.actorOf(Props[TransformationBackend], name = "backend")
//      }
//
//      testConductor.enter("all-started")
//
//      runOn(frontend1, frontend2) {
//        assertServiceOk()
//      }
//
//      testConductor.enter("all-ok")
//    }
  }

//  def assertServiceOk(): Unit = {
//    val transformationFrontend = system.actorSelection("akka://" + system.name + "/user/frontend")
//    // eventually the service should be ok,
//    // backends might not have registered initially
//    awaitAssert {
//      transformationFrontend ! TransformationJob("hello")
//      expectMsgType[TransformationResult](1.second).text should be("HELLO")
//    }
//  }

}
