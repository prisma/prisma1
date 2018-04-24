package com.prisma.akkautil.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.{Error, Ok}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.RejectionError
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.prisma.utils.future.FutureUtils._
import play.api.libs.json._

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.duration._
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

  type StatusCodeValidator = (Int) => Boolean

  private val akkaClient = Http()(system)

  def get(
      uri: String,
      headers: Seq[(String, String)] = Seq.empty,
      statusCodeValidator: StatusCodeValidator = defaultStatusCodeValidator,
      timeout: FiniteDuration = 30.seconds
  ): Future[SimpleHttpResponse] = {
    parseHeaders(headers).flatMap { akkaHeaders =>
      val akkaRequest = HttpRequest(
        uri = uri,
        method = HttpMethods.GET,
        headers = akkaHeaders
      )

      execute(akkaRequest, statusCodeValidator, timeout)
    }
  }

  def postJson[T](
      uri: String,
      body: T,
      headers: Seq[(String, String)] = Seq.empty,
      statusCodeValidator: StatusCodeValidator = defaultStatusCodeValidator,
      timeout: FiniteDuration = 30.seconds
  )(implicit bodyWrites: Writes[T]) = {
    post(uri, Json.toJson(body).toString(), ContentTypes.`application/json`, headers, statusCodeValidator, timeout)
  }

  def post(
      uri: String,
      body: String,
      contentType: ContentType.NonBinary = ContentTypes.`text/plain(UTF-8)`,
      headers: Seq[(String, String)] = Seq.empty,
      statusCodeValidator: StatusCodeValidator = defaultStatusCodeValidator,
      timeout: FiniteDuration = 30.seconds
  ) = {
    parseHeaders(headers).flatMap { akkaHeaders =>
      val akkaRequest = HttpRequest(
        uri = uri,
        method = HttpMethods.POST,
        headers = akkaHeaders,
        entity = HttpEntity(contentType, body)
      )

      execute(akkaRequest, statusCodeValidator, timeout)
    }
  }

  /**
    * Standard validator for status codes. Checks if the code is from 200 - 299.
    *
    * @param statusCode The status code to validate
    * @return true if the status code is considered valid, false otherwise.
    */
  def defaultStatusCodeValidator(statusCode: Int): Boolean = statusCode >= 200 && statusCode < 300

  protected def execute(req: HttpRequest, isValidStatusCode: StatusCodeValidator, timeout: FiniteDuration): Future[SimpleHttpResponse] = {
    val connectionSettings     = ClientConnectionSettings(system).withIdleTimeout(timeout)
    val connectionPoolSettings = ConnectionPoolSettings(system).withConnectionSettings(connectionSettings)

    akkaClient.singleRequest(req, settings = connectionPoolSettings).flatMap { response =>
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
          if (isValidStatusCode(response.status.intValue())) {
            Future.successful(simpleResponse)
          } else {
            Future.failed(FailedResponseCodeError(s"Server responded with ${response.status.intValue()}", simpleResponse))
          }
        }
        .recoverWith {
          case e: RequestFailedError => Future.failed(e)

          case _: RejectionError =>
            val resp = SimpleHttpResponse(response.status.intValue(), None, Seq.empty, response)
            Future.failed(InvalidBodyError(s"Unable to unmarshal response body", resp))

          case e: Throwable =>
            val resp = SimpleHttpResponse(response.status.intValue(), None, Seq.empty, response)
            Future.failed(new RequestFailedError(s"Request failed with: $e", resp))
        }
    }
  }

  /**
    * Parses a collection of headers to akka http headers.
    *
    * @param headers Sequence of tuples representing the headers.
    * @return A future containing a collection of the akka http headers.
    */
  def parseHeaders(headers: Seq[(String, String)]): Future[Seq[HttpHeader]] = Future {
    headers.map { (h: (String, String)) =>
      HttpHeader.parse(h._1, h._2) match {
        case Ok(header, _) => header
        case Error(error)  => sys.error(s"Invalid header: $h | Error: $error")
      }
    }
  }

  def shutdown: Future[Unit] = akkaClient.shutdownAllConnectionPools()
}

case class SimpleHttpResponse(status: Int, body: Option[String], headers: Seq[(String, String)], underlying: HttpResponse) {
  def bodyAs[T](implicit bodyReads: Reads[T]): Try[T] = {
    Try {
      body match {
        case Some(bodyString) =>
          Json.parse(bodyString).asOpt[T] match {
            case Some(value) => value
            case None        => throw InvalidBodyError(s"Invalid body: $bodyString", this)
          }

        case None =>
          throw InvalidBodyError(s"Body is None", this)
      }
    }
  }
}

class RequestFailedError(val reason: String, val response: SimpleHttpResponse)                             extends Exception(reason)
case class FailedResponseCodeError(override val reason: String, override val response: SimpleHttpResponse) extends RequestFailedError(reason, response)
case class InvalidBodyError(override val reason: String, override val response: SimpleHttpResponse)        extends RequestFailedError(reason, response)
