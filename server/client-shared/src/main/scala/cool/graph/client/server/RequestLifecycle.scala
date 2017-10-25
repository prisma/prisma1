package cool.graph.client.server

import cool.graph.client.UserContext
import cool.graph.shared.errors.CommonErrors.InputCompletelyMalformed
import cool.graph.shared.errors.UserAPIErrors.VariablesParsingError
import cool.graph.shared.logging.RequestLogger
import cool.graph.shared.models.{AuthenticatedRequest, Project, ProjectWithClientId}
import cool.graph.utils.`try`.TryUtil
import sangria.parser.QueryParser
import sangria.schema.Schema
import spray.json.{JsArray, JsObject, JsValue}
import spray.json.JsonParser.ParsingException

import scala.util.{Failure, Try}

trait RawRequestAttributes {
  val json: JsValue
  val ip: String
  val sourceHeader: Option[String]
  val logger: RequestLogger
}

case class RawRequest(
    json: JsValue,
    ip: String,
    sourceHeader: Option[String],
    authorizationHeader: Option[String],
    logger: RequestLogger
) extends RawRequestAttributes {

  val id = logger.requestId

  def toGraphQlRequest(
      authorization: Option[AuthenticatedRequest],
      project: ProjectWithClientId,
      schema: Schema[UserContext, Unit]
  ): Try[GraphQlRequest] = {
    val queries: Try[Vector[GraphQlQuery]] = TryUtil.sequence {
      json match {
        case JsArray(requests) => requests.map(GraphQlQuery.tryFromJson)
        case request: JsObject => Vector(GraphQlQuery.tryFromJson(request))
        case malformed         => Vector(Failure(InputCompletelyMalformed(malformed.toString)))
      }
    }
    val isBatch = json match {
      case JsArray(_) => true
      case _          => false
    }
    queries
      .map { queries =>
        GraphQlRequest(
          rawRequest = this,
          authorization = authorization,
          logger = logger,
          projectWithClientId = project,
          schema = schema,
          queries = queries,
          isBatch = isBatch
        )
      }
      .recoverWith {
        case exception => Failure(InvalidGraphQlRequest(exception))
      }
  }
}
case class InvalidGraphQlRequest(underlying: Throwable) extends Exception
// To support Apollos transport-level query batching we treat input and output as a list
// If multiple queries are supplied they are all executed individually and in parallel
// See
// https://dev-blog.apollodata.com/query-batching-in-apollo-63acfd859862#.g733sm6bj
// https://github.com/apollostack/graphql-server/blob/master/packages/graphql-server-core/src/runHttpQuery.ts#L69

case class GraphQlRequest(
    rawRequest: RawRequest,
    authorization: Option[AuthenticatedRequest],
    logger: RequestLogger,
    projectWithClientId: ProjectWithClientId,
    schema: Schema[UserContext, Unit],
    queries: Vector[GraphQlQuery],
    isBatch: Boolean
) extends RawRequestAttributes {
  override val json: JsValue                = rawRequest.json
  override val ip: String                   = rawRequest.ip
  override val sourceHeader: Option[String] = rawRequest.sourceHeader
  val id: String                            = logger.requestId
  val project: Project                      = projectWithClientId.project

}

case class GraphQlQuery(
    query: sangria.ast.Document,
    operationName: Option[String],
    variables: JsValue,
    queryString: String
)

object GraphQlQuery {
  def tryFromJson(requestJson: JsValue): Try[GraphQlQuery] = {
    import spray.json._
    val JsObject(fields) = requestJson
    val query = fields.get("query") match {
      case Some(JsString(query)) => query
      case _                     => ""
    }

    val operationName = fields.get("operationName") collect {
      case JsString(op) if !op.isEmpty ⇒ op
    }

    val variables = fields.get("variables") match {
      case Some(obj: JsObject) => obj
      case Some(JsString(s)) if s.trim.nonEmpty =>
        (try { s.parseJson } catch {
          case e: ParsingException => throw VariablesParsingError(s)
        }) match {
          case json: JsObject => json
          case _              => JsObject.empty
        }
      case _ => JsObject.empty
    }

    QueryParser.parse(query).map { queryAst =>
      GraphQlQuery(
        query = queryAst,
        queryString = query,
        operationName = operationName,
        variables = variables
      )
    }
  }
}
