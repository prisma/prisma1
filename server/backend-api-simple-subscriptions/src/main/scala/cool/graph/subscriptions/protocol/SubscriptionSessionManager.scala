package cool.graph.subscriptions.protocol

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Terminated}
import cool.graph.akkautil.{LogUnhandled, LogUnhandledExceptions}
import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.PubSubPublisher
import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Requests.{InitConnection, SubscriptionSessionRequestV05}
import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Requests.{GqlConnectionInit, SubscriptionSessionRequest}
import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
import cool.graph.subscriptions.protocol.SubscriptionSessionManager.Requests.{EnrichedSubscriptionRequest, EnrichedSubscriptionRequestV05, StopSession}

import scala.collection.mutable

object SubscriptionSessionManager {
  object Requests {
    trait SubscriptionSessionManagerRequest

    case class EnrichedSubscriptionRequestV05(
        sessionId: String,
        projectId: String,
        request: SubscriptionSessionRequestV05
    ) extends SubscriptionSessionManagerRequest

    case class EnrichedSubscriptionRequest(
        sessionId: String,
        projectId: String,
        request: SubscriptionSessionRequest
    ) extends SubscriptionSessionManagerRequest

    case class StopSession(sessionId: String) extends SubscriptionSessionManagerRequest
  }
}

case class SubscriptionSessionManager(subscriptionsManager: ActorRef, bugsnag: BugSnagger)(
    implicit responsePublisher05: PubSubPublisher[SubscriptionSessionResponseV05],
    responsePublisher07: PubSubPublisher[SubscriptionSessionResponse]
) extends Actor
    with LogUnhandledExceptions
    with LogUnhandled {

  val sessions: mutable.Map[String, ActorRef] = mutable.Map.empty

  override def receive: Receive = logUnhandled {
    case EnrichedSubscriptionRequest(sessionId, projectId, request: GqlConnectionInit) =>
      val session = startSessionActorForCurrentProtocolVersion(sessionId, projectId)
      session ! request

    case EnrichedSubscriptionRequest(sessionId, _, request: SubscriptionSessionRequest) =>
      // we might receive session requests that are not meant for this box. So we might not find an actor for this session.
      sessions.get(sessionId).foreach { session =>
        session ! request
      }

    case EnrichedSubscriptionRequestV05(sessionId, projectId, request: InitConnection) =>
      val session = startSessionActorForProtocolVersionV05(sessionId, projectId)
      session ! request

    case EnrichedSubscriptionRequestV05(sessionId, _, request) =>
      // we might receive session requests that are not meant for this box. So we might not find an actor for this session.
      sessions.get(sessionId).foreach { session =>
        session ! request
      }

    case StopSession(sessionId) =>
      sessions.get(sessionId).foreach { session =>
        session ! PoisonPill
        sessions.remove(sessionId)
      }

    case Terminated(terminatedActor) =>
      sessions.find { _._2 == terminatedActor } match {
        case Some((sessionId, _)) => sessions.remove(sessionId)
        case None                 => // nothing to do; should not happen though
      }
  }

  private def startSessionActorForProtocolVersionV05(sessionId: String, projectId: String): ActorRef = {
    val props = Props(SubscriptionSessionActorV05(sessionId, projectId, subscriptionsManager, bugsnag, responsePublisher05))
    startSessionActor(sessionId, props)
  }

  private def startSessionActorForCurrentProtocolVersion(sessionId: String, projectId: String): ActorRef = {
    val props = Props(SubscriptionSessionActor(sessionId, projectId, subscriptionsManager, bugsnag, responsePublisher07))
    startSessionActor(sessionId, props)
  }

  private def startSessionActor(sessionId: String, props: Props): ActorRef = {
    sessions.get(sessionId) match {
      case None =>
        val ref = context.actorOf(props, sessionId)
        sessions += sessionId -> ref
        context.watch(ref)

      case Some(ref) =>
        ref
    }
  }
}
