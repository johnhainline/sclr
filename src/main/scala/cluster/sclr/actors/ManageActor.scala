package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._
import cluster.sclr.core.DatabaseDao
import combinations.Combinations
import combinations.iterators.MultipliedIterator

import scala.concurrent.duration._
import scala.util.Random

class ManageActor(dao: DatabaseDao, r: Random = new Random()) extends Actor with ActorLogging {
  import context.dispatcher

  private var workload: Workload = _
  private var iterator: BufferedIterator[Vector[combinations.Combination]] = _
  private var sendSchedule: Cancellable = _

  private def mediator = DistributedPubSub(context.system).mediator

  private val subscribeSchedule = context.system.scheduler.schedule(
    initialDelay = 0 milliseconds,
    interval = 1 second,
    mediator,
    DistributedPubSubMediator.Subscribe(topicManager, self))

  def receive: Receive = init

  def init: Receive = {
    case SubscribeAck(Subscribe(topicManager, None, `self`)) =>
      subscribeSchedule.cancel()
      log.debug(s"subscribed to topic: $topicManager")
      context.become(waiting)
  }

  def waiting: Receive = {
    case work: Workload =>
      dao.clearDataset(work.name)
      dao.initializeDataset(work.name)
      dao.setupSchemaAndTable(work.name, Y_DIMENSIONS, work.getRowsConstant())
      val datasetInfo = dao.getDatasetInfo(work.name)
      iterator = createIterator(datasetInfo.rowCount, datasetInfo.yLength, work.getRowsConstant(), work.optionalSubset, r)
      workload = work
      log.debug(s"received workload for dataset: ${work.name} with dimensions:${datasetInfo.xLength} rows:${datasetInfo.rowCount} selecting dimensions:$Y_DIMENSIONS rows:${work.getRowsConstant()}")
      context.become(sending)
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.language.postfixOps
      sendSchedule = context.system.scheduler.schedule(0 seconds, 5 seconds, self, workload)
      mediator ! Publish(topicStatus, workload)
      sender() ! Ack
  }

  def sending: Receive = {
    case workload:Workload =>
      mediator ! Publish(topicComputer, workload)
      log.debug(s"workload: ${workload.name} at ${iterator.headOption}")
    case Status =>
      sender() ! StatusResponse(workload, iterator.headOption)
    case GetWork =>
      if (iterator.hasNext) {
        val next = iterator.next()
        sender() ! Work(selectedDimensions = next.head, selectedRows = next.last)
      } else {
        log.debug("Iterator empty, could not GetWork.")
        context.become(finished)
        sendSchedule.cancel()
        sender() ! Finished
        mediator ! Publish(topicStatus, Finished)
      }
  }

  def finished: Receive = {
    case GetWork =>
      sender() ! Finished
  }

  def createIterator(rowCount: Int, yLength: Int, rowsConstant: Int, optionalSubset: Option[Int], r: Random):BufferedIterator[Vector[combinations.Combination]] = {
    val selectYDimensions = () => Combinations(yLength, Y_DIMENSIONS).iterator()
    val selectRows = if (optionalSubset.isEmpty) {
      () => Combinations(rowCount, rowsConstant).iterator()
    } else {
      () => Combinations(rowCount, rowsConstant).subsetIterator(optionalSubset.get, r)
    }
    MultipliedIterator(Vector(selectYDimensions, selectRows)).buffered
  }
}

object ManageActor {
  def props(resultsDao: DatabaseDao) = Props(new ManageActor(resultsDao))
}
