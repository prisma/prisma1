package com.prisma.websocket

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.UnsupportedWebSocketSubprotocolRejection
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.prisma.sangria_server.{RawWebsocketRequest, SangriaWebSocketHandler, WebSocketMessage}
import com.prisma.subscriptions.SubscriptionDependencies
import com.prisma.subscriptions.resolving.SubscriptionsManager
import cool.graph.cuid.Cuid
import play.api.libs.streams.ActorFlow

case class WebSocketHandler(dependencies: SubscriptionDependencies, prefix: String = "")(
    implicit system: ActorSystem,
    materializer: ActorMaterializer,
) extends SangriaWebSocketHandler {

  val v5ProtocolName               = "graphql-subscriptions"
  val v7ProtocolName               = "graphql-ws"
  val supportedProtocols           = Vector(v5ProtocolName, v7ProtocolName)
  private val subscriptionsManager = system.actorOf(Props(new SubscriptionsManager()(dependencies)), "subscriptions-manager")
  private val projectIdEncoder     = dependencies.projectIdEncoder

  // FIXME: this is just here for our subscription tests
  val routes =
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

  override def newWebsocketSession(request: RawWebsocketRequest) = {
    val projectId          = projectIdEncoder.toEncodedString(projectIdEncoder.fromSegments(request.path.toList))
    val isV7               = request.protocol == v7ProtocolName
    val originalFlow       = newSession(projectId, isV7)
    val sangriaHandlerFlow = Flow[WebSocketMessage].map(modelToAkkaWebsocketMessage).via(originalFlow).map(akkaWebSocketMessageToModel)
    sangriaHandlerFlow
  }

  private def newSession(projectId: String, v7protocol: Boolean): Flow[Message, Message, NotUsed] = {
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

  private def modelToAkkaWebsocketMessage(message: WebSocketMessage): Message = TextMessage(message.body)
  private def akkaWebSocketMessageToModel(message: Message) = {
    message match {
      case TextMessage.Strict(body) => WebSocketMessage(body)
      case x                        => sys.error(s"Not supported: $x")
    }
  }
}
