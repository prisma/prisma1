package com.prisma.sangria_server

import akka.NotUsed
import akka.stream.scaladsl.Flow
import cool.graph.cuid.Cuid.createCuid
import play.api.libs.json._
import sangria.parser.QueryParser

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait SangriaServerExecutor {
  def create(handler: SangriaHandler, port: Int, requestPrefix: String): SangriaServer

  def supportsWebsockets: Boolean
}

trait SangriaServer {
  def start(): Future[Unit]
  def startBlocking(): Unit
  def stop(): Future[Unit]

  def requestPrefix: String

  protected def createRequestId(): String = requestPrefix + ":" + createCuid()
}

trait SangriaHandler extends SangriaWebSocketHandler {
  import com.prisma.utils.`try`.TryExtensions._

  def onStart(): Future[Unit]

  def handleRawRequest(request: RawRequest)(implicit ec: ExecutionContext): Future[Response] = {
    request.json match {
      case JsArray(requests) =>
        for {
          graphQlQueries <- Future.sequence(requests.map(parseAsGraphqlQuery(_).toFuture))
          results        <- Future.sequence(graphQlQueries.map(query => handleGraphQlQuery(request, query)))
        } yield {
          Response(JsArray(results))
        }

      case jsonObject: JsObject =>
        for {
          graphQlQuery <- parseAsGraphqlQuery(jsonObject).toFuture
          result       <- handleGraphQlQuery(request, graphQlQuery)
        } yield Response(result)

      case _ =>
//        Vector(Failure(InputCompletelyMalformed(malformed.toString)))
        sys.error("request malformed")
    }
  }

  def handleGraphQlQuery(request: RawRequest, query: GraphQlQuery)(implicit ec: ExecutionContext): Future[JsValue]

  private def parseAsGraphqlQuery(json: JsValue): Try[GraphQlQuery] = Try {
    val result = json match {
      case obj: JsObject =>
        for {
          queryString   <- (obj \ "query").validateOpt[String]
          operationName <- (obj \ "operationName").validateOpt[String]
          variables     <- (obj \ "variables").validateOpt[JsObject]
        } yield {
          val queryAst = QueryParser.parse(queryString.getOrElse("")).get
          GraphQlQuery(
            query = queryAst,
            operationName = operationName,
            variables = variables.getOrElse(JsObject.empty),
            queryString = queryString.getOrElse("")
          )
        }
      case _ =>
        sys.error("not allowed")
    }

    result.get
  }
}

trait SangriaWebSocketHandler {
  def supportedWebsocketProtocols: Vector[String]                                                          = Vector.empty
  def newWebsocketSession(request: RawWebsocketRequest): Flow[WebSocketMessage, WebSocketMessage, NotUsed] = Flow[WebSocketMessage]
}

sealed trait HttpMethod { def name: String }
object HttpMethod {
  object Get  extends HttpMethod { def name = "GET"  }
  object Post extends HttpMethod { def name = "POST" }
}

case class RawRequest(
    id: String,
    method: HttpMethod,
    path: Vector[String],
    headers: Map[String, String],
    json: JsValue,
    ip: String
) {
  val timestampInNanos: Long  = System.nanoTime()
  val timestampInMillis: Long = System.currentTimeMillis()
}

case class RawWebsocketRequest(
    id: String,
    path: Vector[String],
    headers: Map[String, String],
    ip: String,
    protocol: String
)

case class WebSocketMessage(body: String)

case class GraphQlRequest(
    raw: RawRequest,
    queries: Vector[GraphQlQuery]
)

case class GraphQlQuery(
    query: sangria.ast.Document,
    operationName: Option[String],
    variables: JsValue,
    queryString: String
)

case class Response(
    json: JsValue,
    headers: Map[String, String] = Map.empty
)
