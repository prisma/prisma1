package com.prisma.sangria_server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest, RemoteAddress}
import akka.http.scaladsl.server.Directives.{as, entity, extractClientIP, _}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.JsValue

import scala.concurrent.{Await, Future}

object AkkaHttpSangriaServer extends SangriaServerExecutor {
  override def create(server: RequestHandler, port: Int) = AkkaHttpSangriaServer(server, port)
}

case class AkkaHttpSangriaServer(server: RequestHandler, port: Int) extends SangriaServer with PlayJsonSupport {
  import scala.concurrent.duration._

  implicit val system       = ActorSystem()
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val routes = {
    post {
      extractRequest { request =>
        entity(as[JsValue]) { requestJson =>
          extractClientIP { clientIp =>
            val rawRequest = akkaRequestToRawRequest(request, requestJson, clientIp)
            complete(OK -> server.handleRawRequest(rawRequest))
          }
        }
      }
    } ~ get {
      getFromResource("graphiql.html", ContentTypes.`text/html(UTF-8)`)
    }
  }

  private def akkaRequestToRawRequest(req: HttpRequest, json: JsValue, ip: RemoteAddress): RawRequest = {
//    HttpMethods
    val requestId = "" // FIXME: use correct request id
    val reqMethod = req.method match {
      case HttpMethods.GET  => HttpMethod.Get
      case HttpMethods.POST => HttpMethod.Post
      case _                => sys.error("not allowed")
    }
    val headers = req.headers.map(h => h.name -> h.value).toMap
    val path    = req.uri.path.toString.split('/')
    RawRequest(
      id = requestId,
      method = reqMethod,
      path = path.toVector,
      headers = headers,
      json = json,
      ip = ip.toString
    )
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
