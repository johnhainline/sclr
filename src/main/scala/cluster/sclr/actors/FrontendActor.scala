package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent._
import cluster.sclr.actors.FrontendActor.{GetMembershipInfo, MembershipInfo}
import cluster.sclr.http.InfoService

class FrontendActor(infoService: InfoService) extends Actor with ActorLogging {

  var members: Set[Member] = Set.empty

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {
    case GetMembershipInfo =>
      sender() ! MembershipInfo(members)

    case MemberUp(member) ⇒
      log.info("Member is Up: {}", member.address)
      members = members + member
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
