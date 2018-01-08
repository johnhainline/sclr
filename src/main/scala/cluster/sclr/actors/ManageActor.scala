package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.routing.FromConfig
import cluster.sclr.Messages._
import combinations.CombinationAggregation

class ManageActor(combinations: CombinationAggregation) extends Actor with ActorLogging {
  private val iterator = combinations.all()

  val compute = context.actorOf(FromConfig.props(), name = "computeRouter")

  val cluster = Cluster(context.system)

  override def receive: Receive = {
    case Begin => {
      if (iterator.hasNext) {
        val next = iterator.next()
        compute ! Work(next)
      }
    }
  }

}

object ManageActor {
  def props(combinations: CombinationAggregation) = Props(new ManageActor(combinations))
}
