package com.prisma.websocket

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, PoisonPill, Props, ReceiveTimeout, Stash}
import akka.http.scaladsl.model.ws.TextMessage
import com.prisma.akkautil.{LogUnhandled, LogUnhandledExceptions}
import com.prisma.subscriptions.SubscriptionDependencies
import com.prisma.subscriptions.protocol.SubscriptionProtocolV05.Requests.SubscriptionSessionRequestV05
import com.prisma.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import com.prisma.subscriptions.protocol.SubscriptionProtocolV07.Requests.SubscriptionSessionRequest
import com.prisma.subscriptions.protocol.SubscriptionProtocolV07.Responses.{GqlError, SubscriptionSessionResponse}
import com.prisma.subscriptions.protocol.{StringOrInt, SubscriptionSessionActor, SubscriptionSessionActorV05}
import com.prisma.subscriptions.util.PlayJson
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.concurrent.duration._

case class WebsocketSession(
    projectId: String,
    sessionId: String,
    outgoing: ActorRef,
    isV7protocol: Boolean,
    subscriptionsManager: ActorRef
)(implicit dependencies: SubscriptionDependencies)
    extends Actor
    with LogUnhandled
    with LogUnhandledExceptions
    with Stash {
  import metrics.SubscriptionWebsocketMetrics._

  val reporter    = dependencies.reporter
  implicit val ec = context.system.dispatcher

  activeWsConnections.inc
  context.setReceiveTimeout(FiniteDuration(10, TimeUnit.MINUTES))

  context.system.scheduler.schedule(
    dependencies.keepAliveIntervalSeconds.seconds,
    dependencies.keepAliveIntervalSeconds.seconds,
    outgoing,
    if (isV7protocol) {
      TextMessage.Strict("""{"type":"ka"}""")
    } else {
      TextMessage.Strict("""{"type":"keepalive"}""")
    }
  )

  val sessionActor = if (isV7protocol) {
    val props = Props(SubscriptionSessionActor(sessionId, projectId, subscriptionsManager))
    context.actorOf(props, sessionId)
  } else {
    val props = Props(SubscriptionSessionActorV05(sessionId, projectId, subscriptionsManager))
    context.actorOf(props, sessionId)
  }

  def receive: Receive = logUnhandled {
    case TextMessage.Strict(body) =>
      import com.prisma.subscriptions.protocol.ProtocolV05.SubscriptionRequestReaders._
      import com.prisma.subscriptions.protocol.ProtocolV07.SubscriptionRequestReaders._
      import com.prisma.subscriptions.protocol.ProtocolV07.SubscriptionResponseWriters._

      val msg = if (isV7protocol) {
        PlayJson.parse(body).flatMap(_.validate[SubscriptionSessionRequest])
      } else {
        PlayJson.parse(body).flatMap(_.validate[SubscriptionSessionRequestV05])
      }
      msg match {
        case JsSuccess(m, _) => sessionActor ! m
        case JsError(_)      => outgoing ! TextMessage(Json.toJson(GqlError(StringOrInt(string = Some(""), int = None), "The message can't be parsed")).toString())
      }
      incomingWebsocketMessageCount.inc()

    case resp: SubscriptionSessionResponse =>
      import com.prisma.subscriptions.protocol.ProtocolV07.SubscriptionResponseWriters._
      outgoing ! TextMessage(Json.toJson(resp).toString)
      outgoingWebsocketMessageCount.inc()

    case resp: SubscriptionSessionResponseV05 =>
      import com.prisma.subscriptions.protocol.ProtocolV05.SubscriptionResponseWriters._
      outgoing ! TextMessage(Json.toJson(resp).toString)
      outgoingWebsocketMessageCount.inc()

    case ReceiveTimeout =>
      context.stop(self)
  }

  override def postStop = {
    activeWsConnections.dec
    outgoing ! PoisonPill
  }
}
