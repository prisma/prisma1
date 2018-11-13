package com.prisma.sangria_server

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest, RemoteAddress}
import akka.http.scaladsl.server.Directives.{as, entity, extractClientIP, _}
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.http.scaladsl.server.{Route, UnsupportedWebSocketSubprotocolRejection}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BidiFlow, Flow}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.JsValue

import scala.concurrent.{Await, Future}

object AkkaHttpSangriaServer extends SangriaServerExecutor {
  override def create(server: SangriaHandler, port: Int, requestPrefix: String) = AkkaHttpSangriaServer(server, port, requestPrefix)
}

case class AkkaHttpSangriaServer(server: SangriaHandler, port: Int, requestPrefix: String) extends SangriaServer with PlayJsonSupport {
  import scala.concurrent.duration._

  implicit val system       = ActorSystem("sangria-server")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val routes = {
    extractRequest { request =>
      extractClientIP { clientIp =>
        post {
          entity(as[JsValue]) { requestJson =>
            val rawRequest = akkaRequestToRawRequest(request, requestJson, clientIp)
            complete(OK -> server.handleRawRequest(rawRequest))
          }
        } ~ get {
          extractUpgradeToWebSocket { upgrade =>
            upgrade.requestedProtocols.headOption match {
              case Some(protocol) if server.supportedWebsocketProtocols.contains(protocol) =>
                val originalFlow = server.newWebsocketSession(akkaRequestToRawWebsocketRequest(request, clientIp, protocol))
                val akkaHttpFlow = Flow[Message].map(akkaWebSocketMessageToModel).via(originalFlow).map(modelToAkkaWebsocketMessage)
                handleWebSocketMessagesForProtocol(akkaHttpFlow, protocol)
              case _ =>
                reject(UnsupportedWebSocketSubprotocolRejection(server.supportedWebsocketProtocols.head))
            }
          } ~
            getFromResource("graphiql.html", ContentTypes.`text/html(UTF-8)`)
        }
      }
    }
  }

  private def akkaRequestToRawRequest(req: HttpRequest, json: JsValue, ip: RemoteAddress): RawRequest = {
    val reqMethod = req.method match {
      case HttpMethods.GET  => HttpMethod.Get
      case HttpMethods.POST => HttpMethod.Post
      case _                => sys.error("not allowed")
    }
    val headers = req.headers.map(h => h.name -> h.value).toMap
    val path    = req.uri.path.toString.split('/')
    RawRequest(
      id = createRequestId(),
      method = reqMethod,
      path = path.toVector,
      headers = headers,
      json = json,
      ip = ip.toString
    )
  }

  private def akkaRequestToRawWebsocketRequest(req: HttpRequest, ip: RemoteAddress, protocol: String): RawWebsocketRequest = {
    val headers = req.headers.map(h => h.name -> h.value).toMap
    val path    = req.uri.path.toString.split('/')
    RawWebsocketRequest(
      id = createRequestId(),
      path = path.toVector,
      headers = headers,
      ip = ip.toString,
      protocol = protocol
    )
  }

  private def modelToAkkaWebsocketMessage(message: WebSocketMessage): Message = TextMessage(message.body)
  private def akkaWebSocketMessageToModel(message: Message) = {
    message match {
      case TextMessage.Strict(body) => WebSocketMessage(body)
      case x                        => sys.error(s"Not supported: $x")
    }
  }

  lazy val serverBinding: Future[ServerBinding] = {
    val binding = Http().bindAndHandle(Route.handlerFlow(routes), "0.0.0.0", port)
    binding.foreach(b => println(s"Server running on :${b.localAddress.getPort}"))
    binding
  }

  def start: Future[Unit] = serverBinding.map(_ => ())
  def stop: Future[Unit]  = serverBinding.map(_.unbind)

  // Starts the server and blocks the calling thread until the underlying actor system terminates.
  def startBlocking: Unit = {
    start
    Await.result(system.whenTerminated, Duration.Inf)
  }

  def stopBlocking(duration: Duration = 15.seconds): Unit = Await.result(stop, duration)
}
