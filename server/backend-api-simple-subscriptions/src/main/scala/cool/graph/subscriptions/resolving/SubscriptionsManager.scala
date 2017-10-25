package cool.graph.subscriptions.resolving

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.util.Timeout
import cool.graph.akkautil.{LogUnhandled, LogUnhandledExceptions}
import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.PubSubSubscriber
import cool.graph.messagebus.pubsub.Only
import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.subscriptions.protocol.StringOrInt
import cool.graph.subscriptions.resolving.SubscriptionsManager.Requests.CreateSubscription
import cool.graph.subscriptions.resolving.SubscriptionsManagerForProject.SchemaInvalidatedMessage
import play.api.libs.json._
import scaldi.{Injectable, Injector}

import scala.collection.mutable

object SubscriptionsManager {
  object Requests {
    sealed trait SubscriptionsManagerRequest

    case class CreateSubscription(
        id: StringOrInt,
        projectId: String,
        sessionId: String,
        query: sangria.ast.Document,
        variables: Option[JsObject],
        authHeader: Option[String],
        operationName: Option[String]
    ) extends SubscriptionsManagerRequest

    case class EndSubscription(
        id: StringOrInt,
        sessionId: String,
        projectId: String
    ) extends SubscriptionsManagerRequest
  }

  object Responses {
    sealed trait CreateSubscriptionResponse

    case class CreateSubscriptionSucceeded(request: CreateSubscription)                      extends CreateSubscriptionResponse
    case class CreateSubscriptionFailed(request: CreateSubscription, errors: Seq[Exception]) extends CreateSubscriptionResponse
    case class SubscriptionEvent(subscriptionId: StringOrInt, payload: JsValue)
    case class ProjectSchemaChanged(subscriptionId: StringOrInt)
  }

  object Internal {
    case class ResolverType(modelId: String, mutation: ModelMutationType)
  }
}

case class SubscriptionsManager(bugsnag: BugSnagger)(implicit inj: Injector) extends Actor with Injectable with LogUnhandled with LogUnhandledExceptions {

  import SubscriptionsManager.Requests._

  val invalidationSubscriber  = inject[PubSubSubscriber[SchemaInvalidatedMessage]](identified by "schema-invalidation-subscriber")
  implicit val timeout        = Timeout(10, TimeUnit.SECONDS)
  private val projectManagers = mutable.HashMap.empty[String, ActorRef]

  override def receive: Receive = logUnhandled {
    case create: CreateSubscription => projectActorFor(create.projectId).forward(create)
    case end: EndSubscription       => projectActorFor(end.projectId).forward(end)
    case Terminated(ref)            => projectManagers.retain { case (_, projectActor) => projectActor != ref }
  }

  private def projectActorFor(projectId: String): ActorRef = {
    projectManagers.getOrElseUpdate(
      projectId, {
        val ref = context.actorOf(Props(SubscriptionsManagerForProject(projectId, bugsnag)), projectId)
        invalidationSubscriber.subscribe(Only(projectId), ref)
        context.watch(ref)
      }
    )
  }
}
