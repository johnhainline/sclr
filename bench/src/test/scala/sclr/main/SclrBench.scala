package sclr.main

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.Cluster
import org.scalameter.api._
import sclr.core.Messages.Workload
import sclr.core.actors.LifecycleActor.WorkloadEnded

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration.Duration
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.reflect._
import scala.util.Random
import org.scalameter.picklers.Implicits._

object SclrBench extends Bench.OfflineReport {

  val random = new Random(1234)
  val nothing = Gen.enumeration(axisName = "k")(1, 2)

  private case object GetFuture
  class FutureActor[T: ClassTag] extends Actor {
    private val promise = Promise[T]()
    private val clazz = classTag[T].runtimeClass

    override def preStart(): Unit = {
      context.system.eventStream.subscribe(self, clazz)
    }

    override def receive: Receive = {
      case GetFuture =>
        sender() ! promise.future
//        promise.future pipeTo sender()
      case t:T =>
        if (!promise.isCompleted) {
          promise.success(t)
        }
        self ! PoisonPill
    }
  }
  object FutureActor {
    def props[T: ClassTag]() = Props(new FutureActor[T]())
  }

  val workload = Workload(name = "tiny", dnfSize = 2, mu = 0.2, useLPNorm = true)

  var system: ActorSystem = null
  var manageActor: ActorRef = null
  implicit val timeout = Timeout(5 seconds)

  performance of "SCLR" config (
    exec.benchRuns -> 1
    ) in {
    measure method "standard" in {
      using(nothing) setUp { i =>
        system = Sclr.run(Array[String]())
        Thread.sleep(1000)
        manageActor = Await.result(system.actorSelection(path = "/user/manage").resolveOne(), Duration.Inf)
      } tearDown { i =>
        val cluster = Cluster(system)
        cluster.leave(cluster.selfAddress)
        Thread.sleep(1000)
      } in { n =>
          manageActor ! workload

          val futureActor = system.actorOf(FutureActor.props[WorkloadEnded]())
          val future = futureActor ? GetFuture
          val result = Await.result(future, Duration.Inf).asInstanceOf[Future[WorkloadEnded]]
          Await.result(result, Duration.Inf)
      }
    }
  }
}
