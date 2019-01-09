package com.prisma.subscriptions.resolving

import akka.actor.{Actor, ActorRef, Props, Stash, Terminated}
import com.prisma.akkautil.{LogUnhandled, LogUnhandledExceptions}
import com.prisma.messagebus.pubsub.Message
import com.prisma.shared.messages.{SchemaInvalidated, SchemaInvalidatedMessage}
import com.prisma.shared.models._
import com.prisma.subscriptions.SubscriptionDependencies
import com.prisma.subscriptions.helpers.ProjectHelper
import com.prisma.subscriptions.metrics.SubscriptionMetrics
import com.prisma.subscriptions.protocol.StringOrInt
import com.prisma.subscriptions.resolving.SubscriptionsManager.Responses.{CreateSubscriptionFailed, CreateSubscriptionResponse, CreateSubscriptionSucceeded}
import com.prisma.subscriptions.resolving.SubscriptionsManagerForModel.Requests.StartSubscription
import com.prisma.subscriptions.schema.{QueryTransformer, SubscriptionQueryValidator}
import org.scalactic.{Bad, Good}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

case class SubscriptionsManagerForProject(
    projectId: String
)(implicit dependencies: SubscriptionDependencies)
    extends Actor
    with Stash
    with LogUnhandled
    with LogUnhandledExceptions {

  import SubscriptionMetrics._
  import SubscriptionsManager.Requests._
  import akka.pattern.pipe

  val reporter                  = dependencies.reporter
  val resolversByModel          = mutable.Map.empty[Model, ActorRef]
  val resolversBySubscriptionId = mutable.Map.empty[StringOrInt, mutable.Set[ActorRef]]

  override def preStart() = {
    super.preStart()
    activeSubscriptionsManagerForProject.inc
    pipe(ProjectHelper.resolveProject(projectId)(dependencies, context.system, context.dispatcher)) to self
  }

  override def postStop(): Unit = {
    super.postStop()
    activeSubscriptionsManagerForProject.dec
  }

  override def receive: Receive = logUnhandled {
    case project: Project =>
      context.become(ready(project))
      unstashAll()

    case akka.actor.Status.Failure(e) =>
      e.printStackTrace()
      context.stop(self)

    case _ =>
      stash()
  }

  def ready(project: Project): Receive = logUnhandled {
    case create: CreateSubscription =>
      val response = handleSubscriptionCreate(project, create)
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

  def handleSubscriptionCreate(project: Project, job: CreateSubscription): CreateSubscriptionResponse = {
    val model = SubscriptionQueryValidator(project).validate(job.query) match {
      case Good(model) => model
      case Bad(errors) => return CreateSubscriptionFailed(job, errors.map(violation => new Exception(violation.errorMessage)))
    }

    val mutations = QueryTransformer.getMutationTypesFromSubscription(job.query)
    val resolverJob = StartSubscription(
      id = job.id,
      sessionId = job.sessionId,
      query = job.query,
      variables = job.variables,
      operationName = job.operationName,
      mutationTypes = mutations,
      subscriber = sender
    )
    managerForModel(project, model, job.id) ! resolverJob
    CreateSubscriptionSucceeded(job)
  }

  def managerForModel(project: Project, model: Model, subscriptionId: StringOrInt): ActorRef = {
    val resolver = resolversByModel.getOrElseUpdate(
      model, {
        val actorName = model.name
        val ref       = context.actorOf(Props(SubscriptionsManagerForModel(project, model)), actorName)
        context.watch(ref)
      }
    )

    val resolversForSubscriptionId = resolversBySubscriptionId.getOrElseUpdate(subscriptionId, mutable.Set.empty)

    resolversForSubscriptionId.add(resolver)
    resolver
  }

  def removeManagerForModel(ref: ActorRef) = {
    resolversByModel.retain { case (_, resolver)          => resolver != ref }
    resolversBySubscriptionId.retain { case (_, resolver) => resolver != ref }
  }
}
