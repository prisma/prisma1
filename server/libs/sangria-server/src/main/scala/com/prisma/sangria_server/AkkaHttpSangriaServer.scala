package com.prisma.sangria_server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{as, entity, extractClientIP, _}
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.http.scaladsl.server.{ExceptionHandler, Route, UnsupportedWebSocketSubprotocolRejection}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.prisma.akkautil.throttler.Throttler.ThrottlerException
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.JsValue

import scala.concurrent.{Await, Future}

object AkkaHttpSangriaServer extends SangriaServerExecutor {
  override def create(handler: SangriaHandler, port: Int, requestPrefix: String)(implicit system: ActorSystem, materializer: ActorMaterializer) = {
    AkkaHttpSangriaServer(handler, port, requestPrefix)
  }

  override def supportsWebsockets = true
}

case class AkkaHttpSangriaServer(
    handler: SangriaHandler,
    port: Int,
    requestPrefix: String
)(implicit val system: ActorSystem, val materializer: ActorMaterializer)
    extends SangriaServer
    with PlayJsonSupport {
  import system.dispatcher

  import scala.concurrent.duration._

  val routes = {
    handleRejections(CorsDirectives.corsRejectionHandler) {
      cors() {
        extractRequest { request =>
          val requestId = createRequestId()
          respondWithHeader(RawHeader("Request-Id", requestId)) {
            handleExceptions(toplevelExceptionHandler(requestId)) {
              extractClientIP { clientIp =>
                post {
                  entity(as[JsValue]) { requestJson =>
                    val rawRequest = akkaRequestToRawRequest(request, requestJson, clientIp, requestId)
                    onSuccess(handler.handleRawRequest(rawRequest)) { response =>
                      val headers = response.headers.map(h => RawHeader(h._1, h._2)).toVector
                      respondWithHeaders(headers: _*) {
                        complete(OK -> response.json)
                      }
                    }
                  }
                } ~ (get & path("status")) {
                  complete("OK")
                } ~ get {
                  extractUpgradeToWebSocket { upgrade =>
                    upgrade.requestedProtocols.headOption match {
                      case Some(protocol) if handler.supportedWebsocketProtocols.contains(protocol) =>
                        val originalFlow = handler.newWebsocketSession(akkaRequestToRawWebsocketRequest(request, clientIp, protocol, requestId))
                        val akkaHttpFlow = Flow[Message].map(akkaWebSocketMessageToModel).via(originalFlow).map(modelToAkkaWebsocketMessage)
                        handleWebSocketMessagesForProtocol(akkaHttpFlow, protocol)
                      case _ =>
                        reject(UnsupportedWebSocketSubprotocolRejection(handler.supportedWebsocketProtocols.head))
                    }
                  } ~
                    getFromResource("playground.html", ContentTypes.`text/html(UTF-8)`)
                }
              }
            }
          }
        }
      }
    }
  }

  def toplevelExceptionHandler(requestId: String) = ExceptionHandler {
    case e: ThrottlerException =>
      complete(InternalServerError -> JsonErrorHelper.errorJson(requestId, e.getMessage))
    case e: Throwable =>
      println(e.getMessage)
      e.printStackTrace()
      complete(InternalServerError -> JsonErrorHelper.errorJson(requestId, e.getMessage))
  }

  private def akkaRequestToRawRequest(req: HttpRequest, json: JsValue, ip: RemoteAddress, requestId: String): RawRequest = {
    val reqMethod = req.method match {
      case HttpMethods.GET  => HttpMethod.Get
      case HttpMethods.POST => HttpMethod.Post
      case _                => sys.error("not allowed")
    }
    val headers = req.headers.map(h => h.name.toLowerCase() -> h.value).toMap
    val path    = req.uri.path.toString.split('/').filter(_.nonEmpty)
    RawRequest(
      id = requestId,
      method = reqMethod,
      path = path.toVector,
      headers = headers,
      json = json,
      ip = ip.toString
    )
  }

  private def akkaRequestToRawWebsocketRequest(req: HttpRequest, ip: RemoteAddress, protocol: String, requestId: String): RawWebsocketRequest = {
    val headers = req.headers.map(h => h.name.toLowerCase() -> h.value).toMap
    val path    = req.uri.path.toString.split('/')
    RawWebsocketRequest(
      id = requestId,
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
    for {
      _       <- handler.onStart()
      binding <- Http().bindAndHandle(Route.handlerFlow(routes), "0.0.0.0", port)
    } yield {
      println(s"Server running on :${binding.localAddress.getPort}")
      binding
    }
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
