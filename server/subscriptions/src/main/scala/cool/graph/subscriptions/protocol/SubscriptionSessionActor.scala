package cool.graph.subscriptions.protocol

import akka.actor.{Actor, ActorRef}
import cool.graph.akkautil.{LogUnhandled, LogUnhandledExceptions}
import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.PubSubPublisher
import cool.graph.messagebus.pubsub.Only
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
    bugsnag: BugSnagger,
    responsePublisher: PubSubPublisher[SubscriptionSessionResponse]
) extends Actor
    with LogUnhandled
    with LogUnhandledExceptions {

  import SubscriptionMetrics._
  import SubscriptionProtocolV07.Requests._
  import SubscriptionProtocolV07.Responses._
  import cool.graph.subscriptions.resolving.SubscriptionsManager.Requests.CreateSubscription

  override def preStart() = {
    super.preStart()
    activeSubcriptionSessions.inc
  }

  override def postStop(): Unit = {
    super.postStop()
    activeSubcriptionSessions.dec
  }

  override def receive: Receive = logUnhandled {
    case GqlConnectionInit(payload) =>
      ParseAuthorization.parseAuthorization(payload.getOrElse(Json.obj())) match {
        case Some(auth) =>
          publishToResponseQueue(GqlConnectionAck)
          context.become(readyReceive(auth))

        case None =>
          publishToResponseQueue(GqlConnectionError("No Authorization field was provided in payload."))
      }

    case _: SubscriptionSessionRequest =>
      publishToResponseQueue(GqlConnectionError("You have to send an init message before sending anything else."))
  }

  def readyReceive(auth: Authorization): Receive = logUnhandled {
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
