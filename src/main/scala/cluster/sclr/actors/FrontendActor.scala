package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Props, RootActorPath}
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent._
import akka.util.Timeout
import cluster.sclr.actors.FrontendActor.{GetMembershipInfo, MembershipInfo}
import cluster.sclr.http.InfoService

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

class FrontendActor(infoService: InfoService) extends Actor with ActorLogging {

  var members: Set[Member] = Set.empty

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }
  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive = {
    case GetMembershipInfo =>
      sender() ! MembershipInfo(members)

    case MemberUp(member) ⇒
      log.info("Member is Up: {}", member.address)
      members = members + member
      if (member.hasRole("manage")) {
        implicit val timeout = Timeout(30 seconds)
        val selection = context.system.actorSelection(RootActorPath(member.address)/"user"/"manage")
        log.debug(s"found the manage role, using path ${selection.pathString}")
        import scala.concurrent.ExecutionContext.Implicits.global
        Await.ready(selection.resolveOne(), timeout.duration).onComplete {
          case Success(manageActor) => {
            infoService.setManageActor(manageActor)
          }
          case Failure(e) => {
            e.printStackTrace
            throw e
          }
        }
      }
    case UnreachableMember(member) ⇒
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) ⇒
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
      members = members - member
    case _: MemberEvent ⇒ // ignore
  }
}

object FrontendActor {
  case object GetMembershipInfo
  case class MembershipInfo(members: Set[Member])

  def props(infoService: InfoService) = Props(new FrontendActor(infoService))
}
