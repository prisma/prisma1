package cool.graph.websocket

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.UnsupportedWebSocketSubprotocolRejection
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import cool.graph.akkautil.http.Server
import cool.graph.bugsnag.BugSnagger
import cool.graph.cuid.Cuid
import cool.graph.messagebus.pubsub.Everything
import cool.graph.shared.models.ProjectId
import cool.graph.subscriptions.SubscriptionDependencies
import cool.graph.websocket.WebsocketSessionManager.Requests.IncomingQueueMessage
import cool.graph.websocket.metrics.SubscriptionWebsocketMetrics
import play.api.libs.streams.ActorFlow

import scala.concurrent.Future

case class WebsocketServer(dependencies: SubscriptionDependencies, prefix: String = "")(
    implicit system: ActorSystem,
    materializer: ActorMaterializer,
    bugsnag: BugSnagger
) extends Server {
  import SubscriptionWebsocketMetrics._
  import system.dispatcher

  val manager        = system.actorOf(Props(WebsocketSessionManager(dependencies.requestsQueuePublisher, bugsnag)))
  val v5ProtocolName = "graphql-subscriptions"
  val v7ProtocolName = "graphql-ws"

  val responseSubscription = dependencies.responsePubSubSubscriber.subscribe(Everything, { strMsg =>
    incomingResponseQueueMessageRate.inc()
    manager ! IncomingQueueMessage(strMsg.topic, strMsg.payload)
  })

  override def healthCheck: Future[_] = Future.successful(())
  override def onStop: Future[_]      = Future { responseSubscription.unsubscribe }

  val innerRoutes =
    pathPrefix(Segment) { name =>
      pathPrefix(Segment) { stage =>
        get {
          val projectId = ProjectId.toEncodedString(name = name, stage = stage)

          extractUpgradeToWebSocket { upgrade =>
            upgrade.requestedProtocols.headOption match {
              case Some(`v7ProtocolName`) => handleWebSocketMessages(newSession(projectId, v7protocol = true))
              case Some(`v5ProtocolName`) => handleWebSocketMessages(newSession(projectId, v7protocol = false))
              case _                      => reject(UnsupportedWebSocketSubprotocolRejection(v7ProtocolName))
            }
          }
        }
      }
    }

  def newSession(projectId: String, v7protocol: Boolean): Flow[Message, Message, NotUsed] = {
    ActorFlow
      .actorRef[Message, Message] { out =>
        Props {
          WebsocketSession(
            projectId = projectId,
            sessionId = Cuid.createCuid(),
            outgoing = out,
            manager = manager,
            requestsPublisher = dependencies.requestsQueuePublisher,
            bugsnag = bugsnag,
            isV7protocol = v7protocol
          )(dependencies)
        }
      }(system, materializer)
      .mapMaterializedValue(_ => akka.NotUsed)
  }
}
