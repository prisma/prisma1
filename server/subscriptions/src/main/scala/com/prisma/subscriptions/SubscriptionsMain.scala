package com.prisma.subscriptions

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.prisma.errors.ErrorReporter
import com.prisma.akkautil.http.{Routes, Server, ServerExecutor}
import com.prisma.messagebus.pubsub.Only
import com.prisma.subscriptions.protocol.SubscriptionProtocolV05.Requests.SubscriptionSessionRequestV05
import com.prisma.subscriptions.protocol.SubscriptionProtocolV07.Requests.SubscriptionSessionRequest
import com.prisma.subscriptions.protocol.SubscriptionProtocolV07.Responses.GqlError
import com.prisma.subscriptions.protocol.SubscriptionSessionManager.Requests.{EnrichedSubscriptionRequest, EnrichedSubscriptionRequestV05, StopSession}
import com.prisma.subscriptions.protocol.{StringOrInt, SubscriptionRequest, SubscriptionSessionManager}
import com.prisma.subscriptions.resolving.SubscriptionsManager
import com.prisma.subscriptions.util.PlayJson
import com.prisma.websocket.WebsocketServer
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.{JsError, JsSuccess}

import scala.concurrent.Future

object SubscriptionsMain extends App {
  implicit val system       = ActorSystem("graphql-subscriptions")
  implicit val materializer = ActorMaterializer()
  implicit val dependencies = SubscriptionDependenciesImpl()
  import dependencies.reporter

  val subscriptionsServer = SimpleSubscriptionsServer()
  val websocketServer     = WebsocketServer(dependencies)

  ServerExecutor(port = 8086, websocketServer, subscriptionsServer).startBlocking()
}

case class SimpleSubscriptionsServer(prefix: String = "")(
    implicit dependencies: SubscriptionDependencies,
    system: ActorSystem,
    materializer: ActorMaterializer
) extends Server
    with PlayJsonSupport {
  import system.dispatcher

  implicit val response05Publisher = dependencies.responsePubSubPublisherV05
  implicit val response07Publisher = dependencies.responsePubSubPublisherV07

  val innerRoutes          = Routes.emptyRoute
  val subscriptionsManager = system.actorOf(Props(new SubscriptionsManager()), "subscriptions-manager")

  val consumerRef = dependencies.requestsQueueConsumer.withConsumer { req: SubscriptionRequest =>
    Future {
      if (req.body == "STOP") {
        subscriptionSessionManager ! StopSession(req.sessionId)
      } else {
        handleProtocolMessage(req.projectId, req.sessionId, req.body)
      }
    }
  }

  val subscriptionSessionManager = system.actorOf(
    Props(new SubscriptionSessionManager(subscriptionsManager)),
    "subscriptions-sessions-manager"
  )

  def handleProtocolMessage(projectId: String, sessionId: String, messageBody: String) = {
    import com.prisma.subscriptions.protocol.ProtocolV05.SubscriptionRequestReaders._
    import com.prisma.subscriptions.protocol.ProtocolV07.SubscriptionRequestReaders._

    val currentProtocol  = PlayJson.parse(messageBody).flatMap(_.validate[SubscriptionSessionRequest])
    lazy val oldProtocol = PlayJson.parse(messageBody).flatMap(_.validate[SubscriptionSessionRequestV05])

    currentProtocol match {
      case JsSuccess(request, _) =>
        subscriptionSessionManager ! EnrichedSubscriptionRequest(sessionId = sessionId, projectId = projectId, request)

      case JsError(newError) =>
        oldProtocol match {
          case JsSuccess(request, _) =>
            subscriptionSessionManager ! EnrichedSubscriptionRequestV05(sessionId = sessionId, projectId = projectId, request)

          case JsError(oldError) =>
            response07Publisher.publish(Only(sessionId), GqlError(StringOrInt(string = Some(""), int = None), "The message can't be parsed"))
        }
    }
  }

  override def onStop = Future {
    consumerRef.stop
    dependencies.invalidationSubscriber.shutdown
  }
}
