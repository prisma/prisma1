package cool.graph.websocket

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, PoisonPill, Props, ReceiveTimeout, Stash, Terminated}
import akka.http.scaladsl.model.ws.TextMessage
import cool.graph.akkautil.{LogUnhandled, LogUnhandledExceptions}
import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.QueuePublisher
import cool.graph.messagebus.queue.MappingQueuePublisher
import cool.graph.messagebus.testkits.InMemoryQueueTestKit
import cool.graph.websocket.protocol.Request

import scala.collection.mutable
import scala.concurrent.duration._ // if you don't supply your own Protocol (see below)

object WebsocketSessionManager {
  object Requests {
    case class OpenWebsocketSession(projectId: String, sessionId: String, outgoing: ActorRef)
    case class CloseWebsocketSession(sessionId: String)

    case class IncomingWebsocketMessage(projectId: String, sessionId: String, body: String)
    case class IncomingQueueMessage(sessionId: String, body: String)

    case class RegisterWebsocketSession(sessionId: String, actor: ActorRef)
  }

  object Responses {
    case class OutgoingMessage(text: String)
  }
}

case class WebsocketSessionManager(
    requestsPublisher: QueuePublisher[Request],
    bugsnag: BugSnagger
) extends Actor
    with LogUnhandled
    with LogUnhandledExceptions {
  import WebsocketSessionManager.Requests._

  val websocketSessions = mutable.Map.empty[String, ActorRef]

  override def receive: Receive = logUnhandled {
//    case OpenWebsocketSession(projectId, sessionId, outgoing) =>
//      val ref = context.actorOf(Props(WebsocketSession(projectId, sessionId, outgoing, requestsPublisher, bugsnag)))
//      context.watch(ref)
//      websocketSessions += sessionId -> ref
//
//    case CloseWebsocketSession(sessionId) =>
//      websocketSessions.get(sessionId).foreach(context.stop)

    case req: IncomingWebsocketMessage =>
      websocketSessions.get(req.sessionId) match {
        case Some(session) => session ! req
        case None          => println(s"No session actor found for ${req.sessionId} when processing websocket message. This should only happen very rarely.")
      }

    case req: RegisterWebsocketSession =>
      context.watch(req.actor)
      websocketSessions += req.sessionId -> req.actor

    case req: IncomingQueueMessage =>
      websocketSessions.get(req.sessionId) match {
        case Some(session) => session ! req
        case None          => println(s"No session actor found for ${req.sessionId} when processing queue message. This should only happen very rarely.")
      }

    case Terminated(terminatedActor) =>
      websocketSessions.retain {
        case (_, sessionActor) => sessionActor != terminatedActor
      }
  }
}

case class WebsocketSession(
    projectId: String,
    sessionId: String,
    outgoing: ActorRef,
    manager: ActorRef,
    requestsPublisher: QueuePublisher[Request],
    bugsnag: BugSnagger,
    isV7protocol: Boolean
) extends Actor
    with LogUnhandled
    with LogUnhandledExceptions
    with Stash {
  import WebsocketSessionManager.Requests._
  import WebsocketSessionManager.Responses._
  import metrics.SubscriptionWebsocketMetrics._

  implicit val ec = context.system.dispatcher

  activeWsConnections.inc
  context.setReceiveTimeout(FiniteDuration(60, TimeUnit.MINUTES))

  manager ! RegisterWebsocketSession(sessionId, self)

  context.system.scheduler.schedule(10.seconds, 10.seconds, outgoing, if (isV7protocol) {
    TextMessage.Strict("""{"type":"ka"}""")
  } else {
    TextMessage.Strict("""{"type":"keepalive"}""")
  })

  def receive: Receive = logUnhandled {
    case TextMessage.Strict(body)             => println(s"received TextMessage: $body"); requestsPublisher.publish(Request(sessionId, projectId, body))
    case IncomingWebsocketMessage(_, _, body) => println(s"received WebsocketMessage: $body"); requestsPublisher.publish(Request(sessionId, projectId, body))
    case IncomingQueueMessage(_, body)        => println(s"sending out over ws: $body"); outgoing ! TextMessage(body)
    case ReceiveTimeout                       => println(s"received Timeout"); context.stop(self)
  }

  override def postStop = {
    activeWsConnections.dec
    outgoing ! PoisonPill
    requestsPublisher.publish(Request(sessionId, projectId, "STOP"))
  }
}
