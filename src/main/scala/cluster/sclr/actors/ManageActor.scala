package cluster.sclr.actors

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, Props, RootActorPath}
import akka.cluster.ClusterEvent.{InitialStateAsSnapshot, _}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.remote.Ack
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BalanceHub, Keep, Source}
import cluster.sclr.Messages._
import cluster.sclr.core.DatabaseDao
import combinations.Combinations
import combinations.iterators.MultipliedIterator

import scala.collection.mutable
import scala.language.postfixOps
import scala.util.Random

class ManageActor(dao: DatabaseDao, r: Random = new Random()) extends Actor with ActorLogging {
  implicit val mat = ActorMaterializer()(context)

  def receive: Receive = waitingForWorkload
  val cluster = Cluster(context.system)
  val computeMembers = new mutable.HashSet[Member]()
  var source: Source[Work, NotUsed] = _

  def waitingForWorkload: Receive = {
    case workload: Workload =>
      dao.clearDataset(workload.name)
      dao.initializeDataset(workload.name)
      dao.setupSchemaAndTable(workload.name, ManageActor.Y_DIMENSIONS, workload.getRowsConstant())
      val info = dao.getDatasetInfo(workload.name)
      log.debug(s"received workload for dataset: ${workload.name} with dimensions:${info.xLength} rows:${info.rowCount} selecting dimensions:${ManageActor.Y_DIMENSIONS} rows:${workload.getRowsConstant()}")
      sender() ! Ack

      cluster.subscribe(self, initialStateMode = InitialStateAsSnapshot, classOf[MemberEvent], classOf[UnreachableMember])

      // An iterator that runs through (ySize choose 2) * (rows choose 2)
      val iteratorGen = () => ManageActor.createIterator(info.rowCount, info.yLength, workload.getRowsConstant(), workload.optionalSubset, r)
      // A simple producer that runs through our iterator
      val producer = Source.fromIterator(iteratorGen)
      // Attach a BalanceHub Sink to the producer to multiplex. This will materialize to a corresponding Source.
      // (We need to use toMat and Keep.right since by default the materialized value to the left is used)
      val runnableGraph = producer.toMat(BalanceHub.sink())(Keep.right)
      // Create the source that we will attach our "compute" sinks to.
      source = runnableGraph.run()
      context.become(sendingWorkload(workload, source))
  }

  def sendingWorkload(workload: Workload, source: Source[Work, NotUsed])(): Receive = {
    case state: CurrentClusterState =>
      state.members.foreach(processMember(workload))

    case MemberUp(member) =>
      processMember(workload)(member)
    case MemberLeft(member) =>
      processMember(workload)(member)

    case WorkSinkReady(sinkRef) =>
      source.runWith(sinkRef)
  }

  private def processMember(workload: Workload)(member: Member): Unit = {
    if (member.status == MemberStatus.up && !computeMembers.contains(member) && member.hasRole(role = "compute")) {
      val selection = context.actorSelection(RootActorPath(member.address) / "user" / "compute")
      computeMembers.add(member)
      log.debug(s"sending workload to $selection...")
      selection ! workload
    } else if (member.status == MemberStatus.down && computeMembers.contains(member) && member.hasRole(role = "compute")) {
      val selection = context.actorSelection(RootActorPath(member.address) / "user" / "compute")
      computeMembers.remove(member)
      log.debug(s"member $selection left...")
    }
  }
}

object ManageActor {
  private val Y_DIMENSIONS = 2

  def props(resultsDao: DatabaseDao) = Props(new ManageActor(resultsDao))

  private def createIterator(rowCount: Int, yLength: Int, rowsConstant: Int, optionalSubset: Option[Int], r: Random): Iterator[Work] = {
    val selectYDimensions = () => Combinations(yLength, ManageActor.Y_DIMENSIONS).iterator()
    val selectRows = if (optionalSubset.isEmpty) {
      () => Combinations(rowCount, rowsConstant).iterator()
    } else {
      () => Combinations(rowCount, rowsConstant).subsetIterator(optionalSubset.get, r)
    }
    val iterator = MultipliedIterator(Vector(selectYDimensions, selectRows)).map(
      next => Work(selectedDimensions = next.head, selectedRows = next.last)
    )
    iterator
  }
}
