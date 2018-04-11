package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._
import cluster.sclr.core.DatabaseDao
import combinations.{CombinationAggregation, CombinationBuilder, GapSamplingIterator}

import scala.concurrent.duration._
import scala.util.Random

class ManageActor(dao: DatabaseDao, r: Random = new Random()) extends Actor with ActorLogging {

  private var workload: Workload = _
  private var iterator: BufferedIterator[Vector[combinations.Combination]] = _
  private var sendSchedule: Cancellable = _

  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topicManager, self)

  def waiting: Receive = {
    case work: Workload =>
      dao.clearDataset(work.name)
      dao.initializeDataset(work.name)
      dao.setupSchemaAndTable(work.name, Y_DIMENSIONS, work.getRowsConstant())
      val datasetInfo = dao.getDatasetInfo(work.name)
      iterator = createIterator(datasetInfo.rowCount, datasetInfo.yLength, work.getRowsConstant(), work.optionalSample, r)
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
        sender() ! Work(next.head, next.last)
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

  def receive: Receive = waiting

  def createIterator(rowCount: Int, yLength: Int, rowsConstant: Int, optionalSample: Option[Int], r: Random):BufferedIterator[Vector[combinations.Combination]] = {
    val selectYDimensions = CombinationBuilder(yLength, Y_DIMENSIONS)
    val selectRows = CombinationBuilder(rowCount, rowsConstant)
    if (optionalSample.nonEmpty) {
      GapSamplingIterator(CombinationAggregation(Vector(selectYDimensions,selectRows)).all(), rowCount, optionalSample.get, r).buffered
    } else {
      CombinationAggregation(Vector(selectYDimensions,selectRows)).all().buffered
    }
  }
}

object ManageActor {
  def props(resultsDao: DatabaseDao) = Props(new ManageActor(resultsDao))
}
