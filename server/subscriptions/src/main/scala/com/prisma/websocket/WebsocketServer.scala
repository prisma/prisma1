package com.prisma.websocket

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.UnsupportedWebSocketSubprotocolRejection
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.prisma.akkautil.http.Server
import cool.graph.cuid.Cuid
import com.prisma.messagebus.pubsub.Everything
import com.prisma.shared.models.ProjectId
import com.prisma.subscriptions.SubscriptionDependencies
import com.prisma.websocket.WebsocketSessionManager.Requests.IncomingQueueMessage
import com.prisma.websocket.metrics.SubscriptionWebsocketMetrics
import play.api.libs.streams.ActorFlow

import scala.concurrent.Future

case class WebsocketServer(dependencies: SubscriptionDependencies, prefix: String = "")(
    implicit system: ActorSystem,
    materializer: ActorMaterializer,
) extends Server {
  import SubscriptionWebsocketMetrics._
  import dependencies.reporter
  import system.dispatcher

  val manager        = system.actorOf(Props(WebsocketSessionManager(dependencies.requestsQueuePublisher)))
  val v5ProtocolName = "graphql-subscriptions"
  val v7ProtocolName = "graphql-ws"

  val responseSubscription = dependencies.responsePubSubSubscriber.subscribe(Everything, { strMsg =>
    incomingResponseQueueMessageRate.inc()
    manager ! IncomingQueueMessage(strMsg.topic, strMsg.payload)
  })

  override def onStop: Future[_] = Future { responseSubscription.unsubscribe }

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
            isV7protocol = v7protocol
          )(dependencies)
        }
      }(system, materializer)
      .mapMaterializedValue(_ => akka.NotUsed)
  }
}
