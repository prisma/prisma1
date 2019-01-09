package com.prisma.sangria_server

import cats.effect._
import cats.implicits._
import io.circe.Json
import org.http4s
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.server.Server
import org.http4s.server.blaze._
import play.api.libs.json.{JsValue => PlayJsValue}
import ujson.circe.CirceJson
import ujson.play.PlayJson

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object BlazeSangriaServer extends SangriaServerExecutor {
  override def create(handler: SangriaHandler, port: Int, requestPrefix: String) = BlazeSangriaServer(handler, port, requestPrefix)

  override def supportsWebsockets = false
}

case class BlazeSangriaServer(handler: SangriaHandler, port: Int, requestPrefix: String) extends SangriaServer {
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val server = {
    handler.onStart().flatMap { _ =>
      BlazeBuilder[IO]
        .bindHttp(port, "0.0.0.0")
        .withWebSockets(true)
        .mountService(service)
        .start
        .unsafeToFuture()
    }
  }

  def start()         = server.map(_ => ())
  override def stop() = server.map(_.shutdownNow())
  override def startBlocking(): Unit = {
    start()
    Await.result(Future.never, Duration.Inf)
  }

  val service = HttpService[IO] {
    case request if request.method == GET && request.pathInfo == "/status" =>
      Ok("\"OK\"")

    case request if request.method == GET =>
      StaticFile.fromResource("/playground.html", Some(request)).getOrElseF(NotFound())

    case request if request.method == POST =>
      val requestId       = createRequestId()
      val requestIdHeader = Header("Request-Id", requestId)

      val response: IO[http4s.Response[IO]] = for {
        rawRequest <- http4sRequestToRawRequest(request, requestId)
        result     <- IO.fromFuture(IO(handler.handleRawRequest(rawRequest)))
        json       = playJsonToCircleJson(result.json)
        headers    = result.headers.map(h => Header(h._1, h._2)) ++ Vector(requestIdHeader)
        response   <- Ok.apply(json, headers.toSeq: _*)
      } yield response

      response.handleErrorWith { exception =>
        val playJson = JsonErrorHelper.errorJson(requestId, exception.getMessage)
        InternalServerError(playJsonToCircleJson(playJson), requestIdHeader)
      }
  }

  def http4sRequestToRawRequest(request: Request[IO], requestId: String): IO[RawRequest] = {
    request.as[Json].map { json =>
      RawRequest(
        id = requestId,
        method = HttpMethod.Post,
        path = request.uri.path.split('/').filter(_.nonEmpty).toVector,
        headers = request.headers.map(h => h.name.value -> h.value).toMap,
        json = circeJsonToPlayJson(json),
        ip = "0.0.0.0"
      )
    }
  }

  def circeJsonToPlayJson(json: Json): PlayJsValue  = CirceJson.transform(json, PlayJson)
  def playJsonToCircleJson(json: PlayJsValue): Json = PlayJson.transform(json, CirceJson)

}
