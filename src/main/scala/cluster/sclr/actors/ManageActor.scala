package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import cluster.sclr.Messages._
import cluster.sclr.core.DatabaseDao
import combinations.{CombinationAggregation, CombinationBuilder}

import scala.concurrent.duration._

class ManageActor(dao: DatabaseDao) extends Actor with ActorLogging {

  private var workload: Workload = _
  private var iterator: BufferedIterator[Vector[combinations.Combination]] = _
  private var sendSchedule: Cancellable = _

  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topicManager, self)

  def waiting: Receive = {
    case work: Workload =>
      dao.initializeDataset(work.name)
      dao.setupSchemaAndTable(work.name, Y_DIMENSIONS, ROWS)
      val datasetInfo = dao.getDatasetInfo(work.name)
      val selectYDimensions = CombinationBuilder(datasetInfo.yLength, Y_DIMENSIONS)
      val selectRows = CombinationBuilder(datasetInfo.rowCount, ROWS)
      iterator = CombinationAggregation(Vector(selectYDimensions,selectRows)).all().buffered
      workload = work
      log.debug(s"received workload for dataset: ${work.name} with dimensions:${datasetInfo.xLength} rows:${datasetInfo.rowCount} selecting dimensions:$Y_DIMENSIONS rows:$ROWS")
      log.debug("waiting -> sending")
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
        log.debug("sending -> finished")
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
}

object ManageActor {
  def props(resultsDao: DatabaseDao) = Props(new ManageActor(resultsDao))
}
