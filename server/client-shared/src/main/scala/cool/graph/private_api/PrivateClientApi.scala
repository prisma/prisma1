package cool.graph.private_api

import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.Config
import cool.graph.client.finder.RefreshableProjectFetcher
import cool.graph.private_api.schema.PrivateSchemaBuilder
import cool.graph.cuid.Cuid
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models.Project
import cool.graph.util.ErrorHandlerFactory
import cool.graph.util.json.PlaySprayConversions
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.{JsObject, JsValue, Json}
import sangria.ast.Document
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.parser.QueryParser
import scaldi.{Injectable, Injector}

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class GraphQlRequest(query: String, operationName: Option[String] = None, variables: Option[JsValue] = None)

object GraphQlRequest {
  implicit lazy val reads = Json.reads[GraphQlRequest]
}

object PrivateClientApi extends Injectable {
  def apply()(implicit inj: Injector): PrivateClientApi = {
    val projectSchemaFetcher = inject[RefreshableProjectFetcher](identified by "project-schema-fetcher")
    val config               = inject[Config](identified by "config")
    val secret               = config.getString("privateClientApiSecret")

    new PrivateClientApi(projectSchemaFetcher, secret)
  }
}

class PrivateClientApi(projectSchemaFetcher: RefreshableProjectFetcher, secret: String)(implicit inj: Injector)
    extends PlayJsonSupport
    with Injectable
    with PlaySprayConversions {
  import GraphQlRequest.reads
  import sangria.marshalling.playJson._

  import scala.concurrent.ExecutionContext.Implicits.global

  val errorHandlerFactory = ErrorHandlerFactory(println)

  def privateRoute = {
    pathPrefix("private") {
      pathPrefix(Segment) { projectId =>
        post {
          optionalHeaderValueByName("Authorization") { authHeader =>
            if (!authHeader.contains(secret)) {
              complete(Forbidden)
            } else {
              entity(as[GraphQlRequest]) { graphQlRequest =>
                complete {
                  performQuery(projectId, graphQlRequest)
                }
              }
            }
          }
        }
      }
    }
  }

  def performQuery(projectId: String, graphqlRequest: GraphQlRequest): Future[(StatusCode, JsValue)] = {
    QueryParser.parse(graphqlRequest.query) match {
      case Failure(error)    => Future.successful(BadRequest -> Json.obj("error" -> error.getMessage))
      case Success(queryAst) => performQuery(projectId, graphqlRequest, queryAst)
    }
  }

  def performQuery(projectId: String, graphqlRequest: GraphQlRequest, queryAst: Document): Future[(StatusCode, JsValue)] = {
    val GraphQlRequest(query, _, variables) = graphqlRequest
    val requestId                           = Cuid.createCuid()
    val unhandledErrorHandler = errorHandlerFactory.unhandledErrorHandler(
      requestId = requestId,
      query = query,
      variables = variables.getOrElse(Json.obj()).toSpray,
      clientId = None,
      projectId = Some(projectId)
    )

    val sangriaHandler = errorHandlerFactory.sangriaHandler(
      requestId = requestId,
      query = query,
      variables = variables.getOrElse(JsObject.empty).toSpray,
      clientId = None,
      projectId = Some(projectId)
    )

    val result = for {
      project <- getProjectByIdRefreshed(projectId)
      result <- Executor.execute(
                 schema = new PrivateSchemaBuilder(project).build(),
                 queryAst = queryAst,
                 operationName = graphqlRequest.operationName,
                 variables = graphqlRequest.variables.getOrElse(JsObject.empty),
                 exceptionHandler = sangriaHandler
               )
    } yield {
      (OK: StatusCode, result)
    }

    result.recover {
      case error: QueryAnalysisError =>
        (BadRequest, error.resolveError)

      case error: ErrorWithResolver =>
        (InternalServerError, error.resolveError)

      case error =>
        val (statusCode, sprayJson) = unhandledErrorHandler(error)
        (statusCode, sprayJson.toPlay)
    }
  }

  def getProjectByIdRefreshed(projectId: String): Future[Project] = {
    projectSchemaFetcher.fetchRefreshed(projectIdOrAlias = projectId) map {
      case None                      => throw UserAPIErrors.ProjectNotFound(projectId)
      case Some(projectWithClientId) => projectWithClientId.project
    }
  }
}
