package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, Props, RootActorPath}
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent._
import akka.pattern.pipe
import cluster.sclr.http.InfoService

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

class FrontendActor(infoService: InfoService) extends Actor with ActorLogging {
  private case class CheckManageActor(selection: ActorSelection)

  val cluster = Cluster(context.system)
  var manageActorSelection: ActorSelection = _

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }
  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive = {
    case state: CurrentClusterState =>
      log.debug(s"FrontendActor - received CurrentClusterState: $state")
      state.members.foreach(processMember)
    case MemberUp(member) =>
      processMember(member)

    case Success(manageActor: ActorRef) => {
      log.debug("FrontendActor - resolved manageActor")
      infoService.setManageActor(manageActor)
    }
    case Failure(e) => {
      log.error(e, "FrontendActor - could not resolve manageActor")
      requestManageActorUntilReceived()
    }
  }

  private def processMember(member: Member): Unit = {
    if (member.hasRole("manage")) {
      manageActorSelection = context.system.actorSelection(RootActorPath(member.address)/"user"/"manage")
      requestManageActorUntilReceived()
    }
  }

  private def requestManageActorUntilReceived(): Unit = {
    log.debug(s"FrontendActor - trying to resolve $manageActorSelection")
    import scala.concurrent.ExecutionContext.Implicits.global
    manageActorSelection.resolveOne(5 seconds).pipeTo(self)
  }
}

object FrontendActor {
  def props(infoService: InfoService) = Props(new FrontendActor(infoService))
}
