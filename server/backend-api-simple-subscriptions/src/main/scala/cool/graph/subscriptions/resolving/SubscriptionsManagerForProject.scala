package cool.graph.subscriptions.resolving

import akka.actor.{Actor, ActorRef, Props, Stash, Terminated}
import cool.graph.akkautil.{LogUnhandled, LogUnhandledExceptions}
import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.PubSubSubscriber
import cool.graph.messagebus.pubsub.Message
import cool.graph.shared.models._
import cool.graph.subscriptions.helpers.{Auth, ProjectHelper}
import cool.graph.subscriptions.protocol.StringOrInt
import cool.graph.subscriptions.resolving.SubscriptionsManager.Responses.{CreateSubscriptionFailed, CreateSubscriptionResponse, CreateSubscriptionSucceeded}
import cool.graph.subscriptions.resolving.SubscriptionsManagerForModel.Requests.StartSubscription
import cool.graph.subscriptions.resolving.SubscriptionsManagerForProject.{SchemaInvalidated, SchemaInvalidatedMessage}
import cool.graph.subscriptions.schemas.{QueryTransformer, SubscriptionQueryValidator}
import cool.graph.subscriptions.metrics.SubscriptionMetrics
import org.scalactic.{Bad, Good}
import scaldi.Injector
import scaldi.akka.AkkaInjectable
import cool.graph.utils.future.FutureUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object SubscriptionsManagerForProject {
  trait SchemaInvalidatedMessage
  object SchemaInvalidated extends SchemaInvalidatedMessage
}

case class SubscriptionsManagerForProject(
    projectId: String,
    bugsnag: BugSnagger
)(implicit inj: Injector)
    extends Actor
    with Stash
    with AkkaInjectable
    with LogUnhandled
    with LogUnhandledExceptions {

  import SubscriptionsManager.Requests._
  import akka.pattern.pipe
  import SubscriptionMetrics._

  val resolversByModel          = mutable.Map.empty[Model, ActorRef]
  val resolversBySubscriptionId = mutable.Map.empty[StringOrInt, mutable.Set[ActorRef]]

  override def preStart() = {
    super.preStart()
    activeSubscriptionsManagerForProject.inc
    pipe(ProjectHelper.resolveProject(projectId)(inj, context.system, context.dispatcher)) to self
  }

  override def postStop(): Unit = {
    super.postStop()
    activeSubscriptionsManagerForProject.dec
  }

  override def receive: Receive = logUnhandled {
    case project: ProjectWithClientId =>
      context.become(ready(project))
      unstashAll()

    case akka.actor.Status.Failure(e) =>
      e.printStackTrace()
      context.stop(self)

    case _ =>
      stash()
  }

  def ready(project: ProjectWithClientId): Receive = logUnhandled {
    case create: CreateSubscription =>
      val withAuthContext = enrichWithAuthContext(project, create)
      pipe(withAuthContext) to (recipient = self, sender = sender)

    case (create: CreateSubscription, auth) =>
      val response = handleSubscriptionCreate(project, create, auth.asInstanceOf[AuthContext])
      sender ! response

    case end: EndSubscription =>
      resolversBySubscriptionId.getOrElse(end.id, Set.empty).foreach(_ ! end)

    case Terminated(ref) =>
      removeManagerForModel(ref)

    case Message(_, _: SchemaInvalidatedMessage) =>
      context.children.foreach { resolver =>
        resolver ! SchemaInvalidated
      }
      context.stop(self)
  }

  type AuthContext = Try[Option[AuthenticatedRequest]]

  def enrichWithAuthContext(project: ProjectWithClientId, job: CreateSubscription): Future[(CreateSubscription, AuthContext)] = {
    Auth.getAuthContext(project.project, job.authHeader).toFutureTry map { authContext =>
      (job, authContext)
    }
  }

  def handleSubscriptionCreate(project: ProjectWithClientId, job: CreateSubscription, authContext: AuthContext): CreateSubscriptionResponse = {
    val model = SubscriptionQueryValidator(project.project).validate(job.query) match {
      case Good(model) => model
      case Bad(errors) => return CreateSubscriptionFailed(job, errors.map(violation => new Exception(violation.errorMessage)))
    }

    authContext match {
      case Success(userId) =>
        val mutations = QueryTransformer.getMutationTypesFromSubscription(job.query)
        val resolverJob = StartSubscription(
          id = job.id,
          sessionId = job.sessionId,
          query = job.query,
          variables = job.variables,
          operationName = job.operationName,
          mutationTypes = mutations,
          authenticatedRequest = userId,
          subscriber = sender
        )

        managerForModel(project, model, job.id) ! resolverJob
        CreateSubscriptionSucceeded(job)

      case Failure(_) =>
        CreateSubscriptionFailed(job, Seq(new Exception("Could not authenticate with the given auth token")))
    }
  }

  def managerForModel(project: ProjectWithClientId, model: Model, subscriptionId: StringOrInt): ActorRef = {
    val resolver = resolversByModel.getOrElseUpdate(
      model, {
        val actorName = model.name
        val ref       = context.actorOf(Props(SubscriptionsManagerForModel(project, model, bugsnag)), actorName)
        context.watch(ref)
      }
    )

    val resolversForSubscriptionId = resolversBySubscriptionId.getOrElseUpdate(subscriptionId, mutable.Set.empty)

    resolversForSubscriptionId.add(resolver)
    resolver
  }

  def removeManagerForModel(ref: ActorRef) = {
    resolversByModel.retain {
      case (_, resolver) => resolver != ref
    }

    resolversBySubscriptionId.retain {
      case (_, resolver) => resolver != ref
    }
  }
}
