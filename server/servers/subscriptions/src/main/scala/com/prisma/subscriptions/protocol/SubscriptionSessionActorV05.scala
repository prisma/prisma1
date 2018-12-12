package com.prisma.subscriptions.protocol

import akka.actor.{Actor, ActorRef, Stash}
import com.prisma.akkautil.{LogUnhandled, LogUnhandledExceptions}
import com.prisma.shared.models.Project
import com.prisma.subscriptions.SubscriptionDependencies
import com.prisma.subscriptions.helpers.ProjectHelper
import com.prisma.subscriptions.metrics.SubscriptionMetrics
import com.prisma.subscriptions.resolving.SubscriptionsManager.Requests.EndSubscription
import com.prisma.subscriptions.resolving.SubscriptionsManager.Responses.{
  CreateSubscriptionFailed,
  CreateSubscriptionSucceeded,
  ProjectSchemaChanged,
  SubscriptionEvent
}
import play.api.libs.json.Json
import sangria.parser.QueryParser

case class SubscriptionSessionActorV05(
    sessionId: String,
    projectId: String,
    subscriptionsManager: ActorRef
)(implicit dependencies: SubscriptionDependencies)
    extends Actor
    with Stash
    with LogUnhandled
    with LogUnhandledExceptions {

  import context.dispatcher
  import akka.pattern.pipe
  import SubscriptionMetrics._
  import SubscriptionProtocolV05.Requests._
  import SubscriptionProtocolV05.Responses._
  import com.prisma.subscriptions.resolving.SubscriptionsManager.Requests.CreateSubscription

  val auth     = dependencies.auth
  val reporter = dependencies.reporter
  activeSubcriptionSessions.inc

  override def preStart() = {
    super.preStart()
    activeSubcriptionSessions.inc
    pipe(ProjectHelper.resolveProject(projectId)(dependencies, context.system, context.dispatcher)) to self
  }

  override def postStop(): Unit = {
    super.postStop()
    activeSubcriptionSessions.dec
  }

  override def receive: Receive = logUnhandled {
    case project: Project =>
      context.become(waitingForInit(project))
      unstashAll()

    case akka.actor.Status.Failure(e) =>
      e.printStackTrace()
      context.stop(self)

    case _ =>
      stash()
  }

  def waitingForInit(project: Project): Receive = logUnhandled {
    case InitConnection(payload) =>
      ParseAuthorization.parseAuthorization(payload.getOrElse(Json.obj())) match {
        // Case 1: Project has no secrets. Provided auth doesn't matter.
        case x if project.secrets.isEmpty =>
          sendToWebsocket(InitConnectionSuccess)
          context.become(readyReceive(x))

        // Case 2: Project has secrets. Verify provided auth.
        case x @ Some(token) if project.secrets.nonEmpty =>
          val authResult = auth.verifyToken(auth.normalizeToken(token), project.secrets)
          if (authResult.isSuccess) {
            sendToWebsocket(InitConnectionSuccess)
            context.become(readyReceive(x))
          } else {
            sendToWebsocket(InitConnectionFail("Authentication token is invalid."))
          }

        // Case 3: Project has secrets, but no auth provided.
        case None if project.secrets.nonEmpty =>
          sendToWebsocket(InitConnectionFail("No Authorization field was provided in payload."))
      }

    case _: SubscriptionSessionRequestV05 =>
      sendToWebsocket(InitConnectionFail("You have to send an init message before sending anything else."))
  }

  def readyReceive(auth: Option[String]): Receive = logUnhandled {
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
          authHeader = auth,
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
