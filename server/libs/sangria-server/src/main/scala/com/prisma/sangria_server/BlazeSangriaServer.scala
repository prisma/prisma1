package com.prisma.sangria_server

import cats.effect.IO
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.server.Server
import org.http4s.server.blaze._
import play.api.libs.json.{JsValue => PlayJsValue}
import ujson.circe.CirceJson
import ujson.play.PlayJson

import scala.concurrent.Future

object BlazeSangriaServer extends SangriaServerExecutor {
  override def create(handler: SangriaHandler, port: Int, requestPrefix: String) = BlazeSangriaServer(handler, port, requestPrefix)

  override def supportsWebsockets = false
}

case class BlazeSangriaServer(handler: SangriaHandler, port: Int, requestPrefix: String) extends SangriaServer {
  import scala.concurrent.ExecutionContext.Implicits.global

  def start() = {
    BlazeBuilder[IO]
      .bindHttp(port, "0.0.0.0")
      .mountService(service)
      .start
//      .map(onControlC)
      .unsafeRunSync()
    Future.successful(())
  }

  override def startBlocking(): Unit = ???

  override def stop() = Future.successful(())

  val service = HttpService[IO] {
//    case request @ GET -> Root / "status" =>
//      println("status")
//      val x: Message[IO] = request
//      Ok("\"OK\"")
//    case request @ GET =>
//      println("get")
//      StaticFile.fromResource("/graphiql.html", Some(request.asInstanceOf[Request[IO]])).getOrElseF(NotFound())
//
//    case request @ POST =>
//      println("post")
//      val x: IO[Response[IO]] = for {
//        rawRequest <- blazeRequestToRawRequet(request.asInstanceOf[Request[IO]])
////        future: Future[Json] =
//        result   <- IO.fromFuture(IO(handler.handleRawRequest(rawRequest).map(playJsonToCircleJson)))
//        response <- Ok.apply(result)
//      } yield response
//      x

    case request =>
      request.method match {
        case POST =>
          val x: IO[Response[IO]] = for {
            rawRequest <- blazeRequestToRawRequet(request.asInstanceOf[Request[IO]])
            //        future: Future[Json] =
            result   <- IO.fromFuture(IO(handler.handleRawRequest(rawRequest).map(playJsonToCircleJson)))
            response <- Ok.apply(result)
          } yield response
          x
        case GET if request.pathInfo == "/status" => Ok("\"OK\"")
        case GET                                  => StaticFile.fromResource("/graphiql.html", Some(request.asInstanceOf[Request[IO]])).getOrElseF(NotFound())
        case x                                    => NotFound("not found")
      }
  }

  def blazeRequestToRawRequet(request: Request[IO]): IO[RawRequest] = {
    request.as[Json].map { json =>
      RawRequest(
        id = createRequestId(),
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
