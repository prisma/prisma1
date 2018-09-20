package com.prisma.subscriptions.protocol

import akka.actor.{Actor, ActorRef}
import com.prisma.akkautil.{LogUnhandled, LogUnhandledExceptions}
import com.prisma.subscriptions.SubscriptionDependencies
import com.prisma.subscriptions.metrics.SubscriptionMetrics
import com.prisma.subscriptions.protocol.SubscriptionSessionActorV05.Internal.Authorization
import com.prisma.subscriptions.resolving.SubscriptionsManager.Requests.EndSubscription
import com.prisma.subscriptions.resolving.SubscriptionsManager.Responses.{
  CreateSubscriptionFailed,
  CreateSubscriptionSucceeded,
  ProjectSchemaChanged,
  SubscriptionEvent
}
import play.api.libs.json.Json
import sangria.parser.QueryParser

object SubscriptionSessionActorV05 {
  object Internal {
    case class Authorization(token: Option[String])
  }
}
case class SubscriptionSessionActorV05(
    sessionId: String,
    projectId: String,
    subscriptionsManager: ActorRef
)(implicit dependencies: SubscriptionDependencies)
    extends Actor
    with LogUnhandled
    with LogUnhandledExceptions {

  import SubscriptionMetrics._
  import SubscriptionProtocolV05.Requests._
  import SubscriptionProtocolV05.Responses._
  import com.prisma.subscriptions.resolving.SubscriptionsManager.Requests.CreateSubscription

  val reporter = dependencies.reporter
  activeSubcriptionSessions.inc

  override def postStop(): Unit = {
    super.postStop()
    activeSubcriptionSessions.dec
  }

  override def receive: Receive = logUnhandled {
    case InitConnection(payload) =>
      ParseAuthorization.parseAuthorization(payload.getOrElse(Json.obj())) match {
        case Some(auth) =>
          sendToWebsocket(InitConnectionSuccess)
          context.become(readyReceive(auth))

        case None =>
          sendToWebsocket(InitConnectionFail("No Authorization field was provided in payload."))
      }

    case _: SubscriptionSessionRequestV05 =>
      sendToWebsocket(InitConnectionFail("You have to send an init message before sending anything else."))
  }

  def readyReceive(auth: Authorization): Receive = logUnhandled {
    case start: SubscriptionStart =>
      val query = QueryParser.parse(start.query)

      if (query.isFailure) {
        sendToWebsocket(SubscriptionFail(start.id, s"""the GraphQL Query was not valid"""))
      } else {
        val createSubscription = CreateSubscription(
          id = start.id,
          projectId = projectId,
          sessionId = sessionId,
          query = query.get,
          variables = start.variables,
          authHeader = auth.token,
          operationName = SubscriptionSessionActor.Internal.extractOperationName(start.operationName)
        )
        subscriptionsManager ! createSubscription
      }

    case SubscriptionEnd(id) =>
      id.foreach { id =>
        subscriptionsManager ! EndSubscription(id, sessionId, projectId)
      }

    case success: CreateSubscriptionSucceeded =>
      sendToWebsocket(SubscriptionSuccess(success.request.id))

    case fail: CreateSubscriptionFailed =>
      sendToWebsocket(SubscriptionFail(fail.request.id, fail.errors.head.getMessage))

    case SubscriptionEvent(subscriptionId, payload) =>
      sendToWebsocket(SubscriptionData(subscriptionId, payload))

    case ProjectSchemaChanged(subscriptionId) =>
      sendToWebsocket(SubscriptionFail(subscriptionId, "Schema changed"))
  }

  private def sendToWebsocket(response: SubscriptionSessionResponseV05) = {
    context.parent ! response
  }

}
