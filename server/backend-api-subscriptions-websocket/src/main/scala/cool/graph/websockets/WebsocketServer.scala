package cool.graph.websockets

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import cool.graph.akkautil.http.Server
import cool.graph.akkautil.stream.OnCompleteStage
import cool.graph.bugsnag.BugSnagger
import cool.graph.cuid.Cuid
import cool.graph.messagebus.pubsub.Everything
import cool.graph.subscriptions.websockets.services.WebsocketServices
import cool.graph.websockets.WebsocketSessionManager.Requests.IncomingQueueMessage
import metrics.SubscriptionWebsocketMetrics

import scala.concurrent.Future
import scala.concurrent.duration._

case class WebsocketServer(services: WebsocketServices, prefix: String = "")(
    implicit system: ActorSystem,
    materializer: ActorMaterializer,
    bugsnag: BugSnagger
) extends Server {
  import SubscriptionWebsocketMetrics._
  import system.dispatcher

  val manager      = system.actorOf(Props(WebsocketSessionManager(services.requestsQueuePublisher, bugsnag)))
  val subProtocol1 = "graphql-subscriptions"
  val subProtocol2 = "graphql-ws"

  val responseSubscription = services.responsePubSubSubscriber.subscribe(Everything, { strMsg =>
    incomingResponseQueueMessageRate.inc()
    manager ! IncomingQueueMessage(strMsg.topic, strMsg.payload)
  })

  override def healthCheck: Future[_] = Future.successful(())
  override def onStop: Future[_]      = Future { responseSubscription.unsubscribe }

  val innerRoutes = pathPrefix("v1") {
    path(Segment) { projectId =>
      get {
        handleWebSocketMessagesForProtocol(newSession(projectId, v7protocol = false), subProtocol1) ~
          handleWebSocketMessagesForProtocol(newSession(projectId, v7protocol = true), subProtocol2)
      }
    }
  }

  def newSession(projectId: String, v7protocol: Boolean): Flow[Message, Message, NotUsed] = {
    import WebsocketSessionManager.Requests._
    import WebsocketSessionManager.Responses._

    val sessionId = Cuid.createCuid()

    val incomingMessages =
      Flow[Message]
        .collect {
          case TextMessage.Strict(text) ⇒ Future.successful(text)
          case TextMessage.Streamed(textStream) ⇒
            textStream
              .limit(100)
              .completionTimeout(5.seconds)
              .runFold("")(_ + _)
        }
        .mapAsync(3)(identity)
        .map(TextMessage.Strict)
        .collect {
          case TextMessage.Strict(text) =>
            incomingWebsocketMessageRate.inc()
            IncomingWebsocketMessage(projectId = projectId, sessionId = sessionId, body = text)
        }
        .to(Sink.actorRef[IncomingWebsocketMessage](manager, CloseWebsocketSession(sessionId)))

    val outgoingMessage: Source[Message, NotUsed] =
      Source
        .actorRef[OutgoingMessage](5, OverflowStrategy.fail)
        .mapMaterializedValue { outActor =>
          manager ! OpenWebsocketSession(projectId = projectId, sessionId = sessionId, outActor)
          NotUsed
        }
        .map(
          (outMsg: OutgoingMessage) => {
            outgoingWebsocketMessageRate.inc()
            TextMessage(outMsg.text)
          }
        )
        .via(OnCompleteStage(() => {
          manager ! CloseWebsocketSession(sessionId)
        }))
        .keepAlive(FiniteDuration(10, TimeUnit.SECONDS), () => {
          if (v7protocol) {
            TextMessage.Strict("""{"type":"ka"}""")
          } else {
            TextMessage.Strict("""{"type":"keepalive"}""")
          }
        })

    Flow.fromSinkAndSource(incomingMessages, outgoingMessage)
  }
}
