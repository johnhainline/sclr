package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import cluster.sclr.Messages._

import scala.concurrent.Future
import scala.util.Try

class ComputeActor extends Actor with ActorLogging {

  def compute(work: Work): Result = {
    Result(Try {
      Data("blah")
    })
  }

  import context.dispatcher
  def receive = {
    case (work: Work) =>
      val from = sender()
      val future = Future(compute(work)).map { result =>
        log.info("{}! = {} sender: {}", work, result, from.path)
        result
      }
      future.pipeTo(from)
  }
}

object ComputeActor {
  def props() = Props(new ComputeActor())
}
