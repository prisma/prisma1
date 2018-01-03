package cool.graph.websocket

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
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

  val manager      = system.actorOf(Props(WebsocketSessionManager(dependencies.requestsQueuePublisher, bugsnag)))
  val subProtocol1 = "graphql-subscriptions"
  val subProtocol2 = "graphql-ws"

  val responseSubscription = dependencies.responsePubSubSubscriber.subscribe(Everything, { strMsg =>
    incomingResponseQueueMessageRate.inc()
    manager ! IncomingQueueMessage(strMsg.topic, strMsg.payload)
  })

  override def healthCheck: Future[_] = Future.successful(())
  override def onStop: Future[_]      = Future { responseSubscription.unsubscribe }

  val innerRoutes = pathPrefix(Segment) { name =>
    pathPrefix(Segment) { stage =>
      get {
        println("establishing ws connection")
        val projectId = ProjectId.toEncodedString(name = name, stage = stage)
        handleWebSocketMessagesForProtocol(newSession(projectId, v7protocol = false), subProtocol1) ~
          handleWebSocketMessagesForProtocol(newSession(projectId, v7protocol = true), subProtocol2)
      }
    }
  }

  def newSession(projectId: String, v7protocol: Boolean): Flow[Message, Message, NotUsed] = {

    val sessionId = Cuid.createCuid()

//    val flow: Flow[Message, IncomingWebsocketMessage, Any] = ActorFlow
//      .actorRef[Message, Message] { out =>
//        Props(WebsocketSession(projectId, sessionId, out, services.requestsQueuePublisher, bugsnag))
//      }(system, materializer)
//      .collect {
//        case TextMessage.Strict(text) ⇒ Future.successful(text)
//        case TextMessage.Streamed(textStream) ⇒
//          textStream
//            .limit(100)
//            .completionTimeout(5.seconds)
//            .runFold("")(_ + _)
//      }
//      .mapAsync(3)(identity)
//      .map(TextMessage.Strict)
//      .collect {
//        case TextMessage.Strict(text) =>
//          incomingWebsocketMessageRate.inc()
//          IncomingWebsocketMessage(projectId = projectId, sessionId = sessionId, body = text)
//      }
//
//    val x: Sink[Message, Any] = flow.to(Sink.actorRef[IncomingWebsocketMessage](manager, CloseWebsocketSession(sessionId)))

    ActorFlow
      .actorRef[Message, Message] { out =>
        Props(
          WebsocketSession(
            projectId = projectId,
            sessionId = sessionId,
            outgoing = out,
            manager = manager,
            requestsPublisher = dependencies.requestsQueuePublisher,
            bugsnag = bugsnag,
            isV7protocol = v7protocol
          ))
      }(system, materializer)
      .mapMaterializedValue(_ => akka.NotUsed)
//    val incomingMessages =
//      Flow[Message]
//        .collect {
//          case TextMessage.Strict(text) ⇒ Future.successful(text)
//          case TextMessage.Streamed(textStream) ⇒
//            textStream
//              .limit(100)
//              .completionTimeout(5.seconds)
//              .runFold("")(_ + _)
//        }
//        .mapAsync(3)(identity)
//        .map(TextMessage.Strict)
//        .collect {
//          case TextMessage.Strict(text) =>
//            incomingWebsocketMessageRate.inc()
//            IncomingWebsocketMessage(projectId = projectId, sessionId = sessionId, body = text)
//        }
//        .to(Sink.actorRef[IncomingWebsocketMessage](manager, CloseWebsocketSession(sessionId)))
//
//    val outgoingMessage: Source[Message, NotUsed] =
//      Source
//        .actorRef[OutgoingMessage](5, OverflowStrategy.fail)
//        .mapMaterializedValue { outActor =>
//          manager ! OpenWebsocketSession(projectId = projectId, sessionId = sessionId, outActor)
//          NotUsed
//        }
//        .map(
//          (outMsg: OutgoingMessage) => {
//            outgoingWebsocketMessageRate.inc()
//            TextMessage(outMsg.text)
//          }
//        )
//        .via(OnCompleteStage(() => {
//          manager ! CloseWebsocketSession(sessionId)
//        }))
//        .keepAlive(FiniteDuration(10, TimeUnit.SECONDS), () => {
//          if (v7protocol) {
//            TextMessage.Strict("""{"type":"ka"}""")
//          } else {
//            TextMessage.Strict("""{"type":"keepalive"}""")
//          }
//        })
//
//    Flow.fromSinkAndSource(incomingMessages, outgoingMessage)
  }
}
