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
import com.prisma.subscriptions.SubscriptionDependencies
import com.prisma.subscriptions.resolving.SubscriptionsManager
import cool.graph.cuid.Cuid
import play.api.libs.streams.ActorFlow

case class WebsocketServer(dependencies: SubscriptionDependencies, prefix: String = "")(
    implicit system: ActorSystem,
    materializer: ActorMaterializer,
) extends Server {

  val v5ProtocolName       = "graphql-subscriptions"
  val v7ProtocolName       = "graphql-ws"
  val subscriptionsManager = system.actorOf(Props(new SubscriptionsManager()(dependencies)), "subscriptions-manager")

  val innerRoutes =
    pathPrefix(Segments(min = 0, max = 3)) { segments =>
      get {
        val projectId = dependencies.projectIdEncoder.toEncodedString(dependencies.projectIdEncoder.fromSegments(segments))

        extractUpgradeToWebSocket { upgrade =>
          upgrade.requestedProtocols.headOption match {
            case Some(`v7ProtocolName`) => handleWebSocketMessagesForProtocol(newSession(projectId, v7protocol = true), v7ProtocolName)
            case Some(`v5ProtocolName`) => handleWebSocketMessagesForProtocol(newSession(projectId, v7protocol = false), v5ProtocolName)
            case _                      => reject(UnsupportedWebSocketSubprotocolRejection(v7ProtocolName))
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
            isV7protocol = v7protocol,
            subscriptionsManager = subscriptionsManager
          )(dependencies)
        }
      }(system, materializer)
      .mapMaterializedValue(_ => akka.NotUsed)
  }
}
