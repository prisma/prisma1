package cool.graph.akkautil.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.{Error, Ok}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.RejectionError
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import cool.graph.utils.future.FutureUtils._
import play.api.libs.json._

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Simplified abstraction over akka HTTP, allowing to easily execute common HTTP requests, which makes it suitable to
  * most simple use cases without too much setup work.
  *
  * @param system ActorSystem to use.
  * @param materializer ActorMaterializer to use.
  */
case class SimpleHttpClient()(implicit val system: ActorSystem, materializer: ActorMaterializer) {
  import system.dispatcher

  type ResponseUnmarshaller[T] = (HttpResponse) => Future[T]

  private val akkaClient = Http()(system)

  def get(uri: String, headers: Seq[(String, String)] = Seq.empty): Future[SimpleHttpResponse] = {
    parseHeaders(headers).flatMap { akkaHeaders =>
      val akkaRequest = HttpRequest(
        uri = uri,
        method = HttpMethods.GET,
        headers = akkaHeaders
      )

      execute(akkaRequest)
    }
  }

  def postJson[T](uri: String, body: T, headers: Seq[(String, String)] = Seq.empty)(implicit bodyWrites: Writes[T]) = {
    post(uri, Json.toJson(body).toString(), ContentTypes.`application/json`, headers)
  }

  def post(uri: String, body: String, contentType: ContentType.NonBinary = ContentTypes.`text/plain(UTF-8)`, headers: Seq[(String, String)] = Seq.empty) = {
    parseHeaders(headers).flatMap { akkaHeaders =>
      val akkaRequest = HttpRequest(
        uri = uri,
        method = HttpMethods.POST,
        headers = akkaHeaders,
        entity = HttpEntity(contentType, body)
      )

      execute(akkaRequest)
    }
  }

  protected def execute(req: HttpRequest): Future[SimpleHttpResponse] = {
    akkaClient.singleRequest(req).flatMap { response =>
      Unmarshal(response)
        .to[String]
        .toFutureTry
        .flatMap {
          case Success(responseBody) =>
            val headers: Seq[(String, String)] = response.headers.flatMap(HttpHeader.unapply)
            Future.successful(SimpleHttpResponse(response.status.intValue(), Some(responseBody), headers, response))

          case Failure(e) =>
            Future.failed(e)
        }
        .flatMap { simpleResponse =>
          if (response.status.isSuccess()) {
            Future.successful(simpleResponse)
          } else {
            Future.failed(FailedRequestError(s"Server responded with ${response.status.intValue()}", simpleResponse))
          }
        }
        .recoverWith {
          case _: RejectionError =>
            val resp = SimpleHttpResponse(response.status.intValue(), None, Seq.empty, response)
            Future.failed(FailedRequestError(s"Unable to unmarshal response body", resp))

          case e: Throwable =>
            val resp = SimpleHttpResponse(response.status.intValue(), None, Seq.empty, response)
            Future.failed(FailedRequestError(s"Request failed with: $e", resp))
        }
    }
  }

  def parseHeaders(headers: Seq[(String, String)]): Future[Seq[HttpHeader]] = Future {
    headers.map { (h: (String, String)) =>
      HttpHeader.parse(h._1, h._2) match {
        case Ok(header, _) => header
        case Error(error)  => sys.error(s"Invalid header: $h | Error: $error")
      }
    }
  }
}

case class SimpleHttpResponse(status: Int, body: Option[String], headers: Seq[(String, String)], underlying: HttpResponse) {
  def bodyAs[T](implicit bodyReads: Reads[T]): Try[T] = {
    Try {
      body match {
        case Some(bodyString) =>
          Json.parse(bodyString).asOpt[T] match {
            case Some(value) => value
            case None        => sys.error(s"Invalid body: $bodyString")
          }

        case None =>
          sys.error("Body is None")
      }
    }
  }
}

case class FailedRequestError(reason: String, response: SimpleHttpResponse) extends Exception(reason)
