package cool.graph.subscriptions.resolving

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Actor, ActorRef, Stash, Terminated}
import cool.graph.akkautil.{LogUnhandled, LogUnhandledExceptions}
import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.PubSubSubscriber
import cool.graph.messagebus.pubsub.{Message, Only, Subscription}
import cool.graph.metrics.GaugeMetric
import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.shared.models._
import cool.graph.subscriptions.metrics.SubscriptionMetrics
import cool.graph.subscriptions.protocol.StringOrInt
import cool.graph.subscriptions.resolving.SubscriptionsManager.Requests.EndSubscription
import cool.graph.subscriptions.resolving.SubscriptionsManager.Responses.{ProjectSchemaChanged, SubscriptionEvent}
import cool.graph.subscriptions.resolving.SubscriptionsManagerForProject.SchemaInvalidated
import play.api.libs.json._
import sangria.ast.Document
import sangria.renderer.QueryRenderer
import scaldi.Injector
import scaldi.akka.AkkaInjectable

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}

object SubscriptionsManagerForModel {
  object Requests {
    case class StartSubscription(
        id: StringOrInt,
        sessionId: String,
        query: Document,
        variables: Option[JsObject],
        operationName: Option[String],
        mutationTypes: Set[ModelMutationType],
        authenticatedRequest: Option[AuthenticatedRequest],
        subscriber: ActorRef
    ) {
      lazy val queryAsString: String = QueryRenderer.render(query)
    }
  }

  object Internal {
    case class SubscriptionId(
        id: StringOrInt,
        sessionId: String
    )
  }
}

case class SubscriptionsManagerForModel(
    project: ProjectWithClientId,
    model: Model,
    bugsnag: BugSnagger
)(implicit inj: Injector)
    extends Actor
    with Stash
    with AkkaInjectable
    with LogUnhandled
    with LogUnhandledExceptions
    with MutationChannelUtil {

  import SubscriptionMetrics._
  import SubscriptionsManagerForModel.Internal._
  import SubscriptionsManagerForModel.Requests._
  import context.dispatcher

  val projectId                = project.project.id
  val subscriptions            = mutable.Map.empty[SubscriptionId, StartSubscription]
  val smartActiveSubscriptions = SmartGaugeMetric(activeSubscriptions)
  val pubSubSubscriptions      = ListBuffer[Subscription]()
  val sssEventsSubscriber      = inject[PubSubSubscriber[String]](identified by "sss-events-subscriber")

  override def preStart() = {
    super.preStart()

    activeSubscriptionsManagerForModelAndMutation.inc
    smartActiveSubscriptions.set(0)

    pubSubSubscriptions ++= mutationChannelsForModel(projectId, model).map { channel =>
      sssEventsSubscriber.subscribe(Only(channel), self)
    }
  }

  override def postStop(): Unit = {
    super.postStop()

    activeSubscriptionsManagerForModelAndMutation.dec
    smartActiveSubscriptions.set(0)
    pubSubSubscriptions.foreach(_.unsubscribe)
    pubSubSubscriptions.clear()
  }

  override def receive = logUnhandled {
    case start: StartSubscription =>
      val subscriptionId = SubscriptionId(start.id, start.sessionId)
      subscriptions += (subscriptionId -> start)
      smartActiveSubscriptions.set(subscriptions.size)
      context.watch(start.subscriber)

    case end: EndSubscription =>
      val subcriptionId = SubscriptionId(id = end.id, sessionId = end.sessionId)
      subscriptions -= subcriptionId
      smartActiveSubscriptions.set(subscriptions.size)

    case Message(topic: String, message: String) =>
      databaseEventRate.inc(projectId)
      val mutationType = this.extractMutationTypeFromChannel(topic, model)
      handleDatabaseMessage(message, mutationType)

    case SchemaInvalidated =>
      subscriptions.values.foreach { subscription =>
        subscription.subscriber ! ProjectSchemaChanged(subscription.id)
      }

    case Terminated(subscriber) =>
      handleTerminatedSubscriber(subscriber)
  }

  def handleDatabaseMessage(eventStr: String, mutationType: ModelMutationType): Unit = {
    import cool.graph.utils.future.FutureUtils._

    val subscriptionsForMutationType = subscriptions.values.filter(_.mutationTypes.contains(mutationType))

    // We need to take query variables into consideration - group by query and variables
    val groupedSubscriptions: Map[(String, String), Iterable[StartSubscription]] =
      subscriptionsForMutationType.groupBy(sub => (sub.queryAsString, sub.variables.getOrElse("").toString))

    val optimizedProcessEventFns = groupedSubscriptions.flatMap {
      case (_, subscriptionsWithSameQuery) =>
        // only if the subscription has authentication and the model is actually using permissions queries we have to execute each subscription on its own
        val (subscriptionsThatMustBeDoneEach, subscriptionsThatCanBeDoneOnlyOnce) = subscriptionsWithSameQuery.partition { subscription =>
          subscription.authenticatedRequest.isDefined && model.hasQueryPermissions
        }

        val performEach: Iterable[() => Future[Unit]] = subscriptionsThatMustBeDoneEach.map { subscription =>
          processDatabaseAndNotifySubscribersEventFn(
            eventStr = eventStr,
            subscriptionToExecute = subscription,
            subscriptionsToNotify = Vector(subscription),
            mutationType = mutationType
          )
        }

        val performOnlyTheFirstAndReuseResult: Option[() => Future[Unit]] = subscriptionsThatCanBeDoneOnlyOnce.headOption.map { subscription =>
          processDatabaseAndNotifySubscribersEventFn(
            eventStr = eventStr,
            subscriptionToExecute = subscription,
            subscriptionsToNotify = subscriptionsThatCanBeDoneOnlyOnce,
            mutationType = mutationType
          )
        }

        performOnlyTheFirstAndReuseResult ++ performEach
    }

    optimizedProcessEventFns.toList.runInChunksOf(maxParallelism = 10)
  }

  def processDatabaseAndNotifySubscribersEventFn(
      eventStr: String,
      subscriptionToExecute: StartSubscription,
      subscriptionsToNotify: Iterable[StartSubscription],
      mutationType: ModelMutationType
  ): () => Future[Unit] = { () =>
    handleDatabaseEventRate.inc(projectId)

    val result = processDatabaseEventForSubscription(eventStr, subscriptionToExecute, mutationType)
    result.onComplete {
      case Success(x) => subscriptionsToNotify.foreach(sendDataToSubscriber(_, x))
      case Failure(e) => e.printStackTrace()
    }

    result.map(_ => ())
  }

  /**
    * This is a separate method so it can be stubbed in tests.
    */
  def processDatabaseEventForSubscription(
      event: String,
      subscription: StartSubscription,
      mutationType: ModelMutationType
  ): Future[Option[JsValue]] = {
    SubscriptionResolver(project, model, mutationType, subscription, context.system.scheduler).handleDatabaseMessage(event)
  }

  def sendDataToSubscriber(subscription: StartSubscription, value: Option[JsValue]): Unit = {
    value.foreach { json =>
      val response = SubscriptionEvent(subscription.id, json)
      subscription.subscriber ! response
    }
  }

  def handleTerminatedSubscriber(subscriber: ActorRef) = {
    subscriptions.retain { case (_, job) => job.subscriber != subscriber }
    smartActiveSubscriptions.set(subscriptions.size)

    if (subscriptions.isEmpty) {
      context.stop(self)
    }
  }
}

case class SmartGaugeMetric(gaugeMetric: GaugeMetric) {
  val value = new AtomicLong(0)

  def set(newValue: Long): Unit = {
    val delta = newValue - value.get()
    gaugeMetric.add(delta)
    value.set(newValue)
  }
}
