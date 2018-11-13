package com.prisma.sangria_server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.utils.`try`.TryUtil
import play.api.libs.json._
import sangria.parser.QueryParser

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

trait SangriaServerExecutor {
  def create(server: RequestHandler, port: Int): SangriaServer
}

trait SangriaServer {
  def start(): Future[Unit]
  def startBlocking(): Unit
  def stop(): Future[Unit]
}

trait RequestHandler {
  import com.prisma.utils.`try`.TryExtensions._

  def handleRawRequest(request: RawRequest)(implicit ec: ExecutionContext): Future[JsValue] = {
    request.json match {
      case JsArray(requests) =>
        for {
          graphQlQueries <- Future.sequence(requests.map(parseAsGraphqlQuery(_).toFuture))
          results        <- Future.sequence(graphQlQueries.map(query => handleGraphQlQuery(request, query)))
        } yield JsArray(results)

      case jsonObject: JsObject =>
        for {
          graphQlQuery <- parseAsGraphqlQuery(jsonObject).toFuture
          result       <- handleGraphQlQuery(request, graphQlQuery)
        } yield result

      case malformed =>
//        Vector(Failure(InputCompletelyMalformed(malformed.toString)))
        sys.error("request malformed")
    }
  }

  def handleGraphQlQuery(request: RawRequest, query: GraphQlQuery)(implicit ec: ExecutionContext): Future[JsValue]

  private def parseAsGraphqlQuery(json: JsValue): Try[GraphQlQuery] = Try {
    // FIXME: remove those .get calls
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

sealed trait HttpMethod
object HttpMethod {
  object Get  extends HttpMethod
  object Post extends HttpMethod
}

case class RawRequest(
    id: String,
    method: HttpMethod,
    path: Vector[String],
    headers: Map[String, String],
    json: JsValue,
    ip: String
)

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
