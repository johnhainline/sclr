package sclr.core.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster

// This class is responsible for shutting down the actor system after a job has completed.
class LifecycleActor extends Actor with ActorLogging {
  import LifecycleActor._
  private var started = false
  private var manageActorOption: Option[ActorRef] = None
  private var computeActorOption: Option[ActorRef] = None
  private var computeActorDone = false
  private var activeManage = 0
  private var activeCompute = 0

  def checkForShutdown(): Unit = {
    val computeDone = computeActorOption.isEmpty || (computeActorOption.nonEmpty && activeCompute == 0 && computeActorDone)
    val manageDone  = manageActorOption.isEmpty  || (manageActorOption.nonEmpty  && activeManage  == 0)
    if (started && computeDone && manageDone) {
      computeActorOption.foreach { actor =>
        log.info(s"ComputeActor ($actor) no longer active.")
      }
      manageActorOption.foreach { actor =>
        log.info(s"ManageActor ($actor) no longer active.")
      }
      log.info(s"Leaving Cluster...")
      val cluster = Cluster(context.system)
      cluster.leave(cluster.selfAddress)
    }
  }

  def receive: Receive = {
    case ComputeActorDone(computeActor) =>
      computeActorDone = true
      checkForShutdown()


    case ComputeStreamStarted(computeActor, instance) =>
      started = true
      computeActorOption = Some(computeActor)
      activeCompute += 1

    case ComputeStreamCompleted(computeActor, instance) =>
      activeCompute -= 1
      checkForShutdown()

    case ComputeStreamFailed(computeActor, instance, e) =>
      activeCompute -= 1
      checkForShutdown()


    case ManageStreamStarted(manageActor) =>
      started = true
      manageActorOption = Some(manageActor)
      activeManage += 1

    case ManageStreamCompleted(manageActor) =>
      activeManage -= 1
      checkForShutdown()

    case ManageStreamFailed(manageActor, e) =>
      activeManage -= 1
      checkForShutdown()
  }
}

object LifecycleActor {
  trait ComputeMessage
  final case class ComputeActorDone(computeActor: ActorRef) extends ComputeMessage

  final case class ComputeStreamStarted(computeActor: ActorRef, instance: Int) extends ComputeMessage
  final case class ComputeStreamCompleted(computeActor: ActorRef, instance: Int) extends ComputeMessage
  final case class ComputeStreamFailed(computeActor: ActorRef, instance: Int, e: Throwable) extends ComputeMessage

  trait ManageMessage
  final case class ManageStreamStarted(manageActor: ActorRef) extends ManageMessage
  final case class ManageStreamCompleted(manageActor: ActorRef) extends ManageMessage
  final case class ManageStreamFailed(manageActor: ActorRef, e: Throwable) extends ManageMessage

  def props() = Props(new LifecycleActor())
}
