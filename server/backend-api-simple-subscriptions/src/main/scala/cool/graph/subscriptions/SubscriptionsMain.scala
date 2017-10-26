package cool.graph.subscriptions

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.{Routes, Server, ServerExecutor}
import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.pubsub.Only
import cool.graph.messagebus.{PubSubPublisher, PubSubSubscriber, QueueConsumer}
import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Requests.SubscriptionSessionRequestV05
import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Responses.SubscriptionSessionResponseV05
import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Requests.SubscriptionSessionRequest
import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Responses.{GqlError, SubscriptionSessionResponse}
import cool.graph.subscriptions.protocol.SubscriptionSessionManager.Requests.{EnrichedSubscriptionRequest, EnrichedSubscriptionRequestV05, StopSession}
import cool.graph.subscriptions.protocol.{StringOrInt, SubscriptionRequest, SubscriptionSessionManager}
import cool.graph.subscriptions.resolving.SubscriptionsManager
import cool.graph.subscriptions.resolving.SubscriptionsManagerForProject.SchemaInvalidatedMessage
import cool.graph.subscriptions.util.PlayJson
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.{JsError, JsSuccess}
import scaldi.akka.AkkaInjectable
import scaldi.{Injectable, Injector}

import scala.concurrent.Future

object SubscriptionsMain extends App with Injectable {
  implicit val system       = ActorSystem("graphql-subscriptions")
  implicit val materializer = ActorMaterializer()
  implicit val inj          = SimpleSubscriptionDependencies()

  ServerExecutor(port = 8086, SimpleSubscriptionsServer()).startBlocking()
}

case class SimpleSubscriptionsServer(prefix: String = "")(
    implicit inj: Injector,
    system: ActorSystem,
    materializer: ActorMaterializer
) extends Server
    with AkkaInjectable
    with PlayJsonSupport {
  import system.dispatcher

  implicit val bugSnag             = inject[BugSnagger]
  implicit val response05Publisher = inject[PubSubPublisher[SubscriptionSessionResponseV05]](identified by "subscription-responses-publisher-05")
  implicit val response07Publisher = inject[PubSubPublisher[SubscriptionSessionResponse]](identified by "subscription-responses-publisher-07")

  val innerRoutes          = Routes.emptyRoute
  val subscriptionsManager = system.actorOf(Props(new SubscriptionsManager(bugSnag)), "subscriptions-manager")
  val requestsConsumer     = inject[QueueConsumer[SubscriptionRequest]](identified by "subscription-requests-consumer")

  val consumerRef = requestsConsumer.withConsumer { req: SubscriptionRequest =>
    Future {
      if (req.body == "STOP") {
        subscriptionSessionManager ! StopSession(req.sessionId)
      } else {
        handleProtocolMessage(req.projectId, req.sessionId, req.body)
      }
    }
  }

  val subscriptionSessionManager = system.actorOf(
    Props(new SubscriptionSessionManager(subscriptionsManager, bugSnag)),
    "subscriptions-sessions-manager"
  )

  def handleProtocolMessage(projectId: String, sessionId: String, messageBody: String) = {
    import cool.graph.subscriptions.protocol.ProtocolV05.SubscriptionRequestReaders._
    import cool.graph.subscriptions.protocol.ProtocolV07.SubscriptionRequestReaders._

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

  override def healthCheck: Future[_] = Future.successful(())

  override def onStop = Future {
    consumerRef.stop
    inject[PubSubSubscriber[SchemaInvalidatedMessage]](identified by "schema-invalidation-subscriber").shutdown
  }
}
