package cool.graph.subscriptions.protocol

import akka.actor.{Actor, ActorRef, Stash}
import cool.graph.akkautil.{LogUnhandled, LogUnhandledExceptions}
import cool.graph.auth.AuthImpl
import cool.graph.messagebus.PubSubPublisher
import cool.graph.messagebus.pubsub.Only
import cool.graph.shared.models.{Project, ProjectWithClientId}
import cool.graph.subscriptions.SubscriptionDependencies
import cool.graph.subscriptions.helpers.ProjectHelper
import cool.graph.subscriptions.metrics.SubscriptionMetrics
import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Responses.SubscriptionSessionResponse
import cool.graph.subscriptions.protocol.SubscriptionSessionActorV05.Internal.Authorization
import cool.graph.subscriptions.resolving.SubscriptionsManager.Requests.EndSubscription
import cool.graph.subscriptions.resolving.SubscriptionsManager.Responses.{
  CreateSubscriptionFailed,
  CreateSubscriptionSucceeded,
  ProjectSchemaChanged,
  SubscriptionEvent
}
import play.api.libs.json._
import sangria.parser.QueryParser

object SubscriptionSessionActor {
  object Internal {
    case class Authorization(token: Option[String])

    // see https://github.com/apollographql/subscriptions-transport-ws/issues/174
    def extractOperationName(operationName: Option[String]): Option[String] = operationName match {
      case Some("") => None
      case x        => x
    }
  }
}

case class SubscriptionSessionActor(
    sessionId: String,
    projectId: String,
    subscriptionsManager: ActorRef,
    responsePublisher: PubSubPublisher[SubscriptionSessionResponse]
)(implicit dependencies: SubscriptionDependencies)
    extends Actor
    with LogUnhandled
    with LogUnhandledExceptions
    with Stash {

  import SubscriptionMetrics._
  import SubscriptionProtocolV07.Requests._
  import SubscriptionProtocolV07.Responses._
  import akka.pattern.pipe
  import context.dispatcher
  import cool.graph.subscriptions.resolving.SubscriptionsManager.Requests.CreateSubscription

  val reporter = dependencies.reporter

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
    case project: ProjectWithClientId =>
      context.become(waitingForInit(project.project))
      unstashAll()

    case akka.actor.Status.Failure(e) =>
      e.printStackTrace()
      context.stop(self)

    case _ =>
      stash()
  }

  def waitingForInit(project: Project): Receive = logUnhandled {
    case GqlConnectionInit(payload) =>
      ParseAuthorization.parseAuthorization(payload.getOrElse(Json.obj())) match {
        case Some(auth) =>
          val authResult = AuthImpl.verify(project.secrets, auth.token)
          if (authResult.isSuccess) {
            publishToResponseQueue(GqlConnectionAck)
            context.become(initFinishedReceive(auth))
          } else {
            publishToResponseQueue(GqlConnectionError("Authentication token is invalid."))
          }

        case None =>
          publishToResponseQueue(GqlConnectionError("No Authorization field was provided in payload."))
      }

    case _: SubscriptionSessionRequest =>
      publishToResponseQueue(GqlConnectionError("You have to send an init message before sending anything else."))
  }

  def initFinishedReceive(auth: Authorization): Receive = logUnhandled {
    case GqlStart(id, payload) =>
      handleStart(id, payload, auth)

    case GqlStop(id) =>
      subscriptionsManager ! EndSubscription(id, sessionId, projectId)

    case success: CreateSubscriptionSucceeded =>
    // FIXME: this is really a NO-OP now?

    case fail: CreateSubscriptionFailed =>
      publishToResponseQueue(GqlError(fail.request.id, fail.errors.head.getMessage))

    case ProjectSchemaChanged(subscriptionId) =>
      publishToResponseQueue(GqlError(subscriptionId, "Schema changed"))

    case SubscriptionEvent(subscriptionId, payload) =>
      val response = GqlData(subscriptionId, payload)
      publishToResponseQueue(response)
  }

  private def handleStart(id: StringOrInt, payload: GqlStartPayload, auth: Authorization) = {
    val query = QueryParser.parse(payload.query)

    if (query.isFailure) {
      publishToResponseQueue(GqlError(id, s"""the GraphQL Query was not valid"""))
    } else {
      val createSubscription = CreateSubscription(
        id = id,
        projectId = projectId,
        sessionId = sessionId,
        query = query.get,
        variables = payload.variables,
        authHeader = auth.token,
        operationName = SubscriptionSessionActor.Internal.extractOperationName(payload.operationName)
      )
      subscriptionsManager ! createSubscription
    }
  }

  private def publishToResponseQueue(response: SubscriptionSessionResponse) = {
    responsePublisher.publish(Only(sessionId), response)
  }
}

object ParseAuthorization {
  def parseAuthorization(jsObject: JsObject): Option[Authorization] = {

    def parseLowerCaseAuthorization = {
      (jsObject \ "authorization").validateOpt[String] match {
        case JsSuccess(authField, _) => Some(Authorization(authField))
        case JsError(_)              => None
      }
    }

    (jsObject \ "Authorization").validateOpt[String] match {
      case JsSuccess(Some(auth), _) => Some(Authorization(Some(auth)))
      case JsSuccess(None, _)       => parseLowerCaseAuthorization
      case JsError(_)               => None
    }
  }
}
