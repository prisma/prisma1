package cool.graph.api.server

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model._
import com.prisma.errors.{ErrorReporter, ProjectMetadata}
import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataResolver
import cool.graph.api.database.import_export.{BulkExport, BulkImport}
import cool.graph.api.project.ProjectFetcher
import cool.graph.api.schema.APIErrors.InvalidToken
import cool.graph.api.schema._
import cool.graph.auth.Auth
import cool.graph.client.server.GraphQlRequestHandler
import cool.graph.shared.models.{Project, ProjectWithClientId}
import cool.graph.utils.`try`.TryExtensions._
import sangria.schema.Schema
import spray.json.{JsObject, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

case class RequestHandler(
    projectFetcher: ProjectFetcher,
    schemaBuilder: SchemaBuilder,
    graphQlRequestHandler: GraphQlRequestHandler,
    auth: Auth,
    log: Function[String, Unit]
)(implicit reporter: ErrorReporter, ec: ExecutionContext, apiDependencies: ApiDependencies) {

  def handleRawRequestForPublicApi(
      projectId: String,
      rawRequest: RawRequest
  ): Future[(StatusCode, JsValue)] = {
    handleRawRequestWithSchemaBuilder(projectId, rawRequest) { project =>
      schemaBuilder(project)
    }
  }

  def handleRawRequestForPrivateApi(projectId: String, rawRequest: RawRequest): Future[(StatusCode, JsValue)] = {
    handleRawRequestWithSchemaBuilder(projectId, rawRequest) { project =>
      PrivateSchemaBuilder(project)(apiDependencies, apiDependencies.system).build()
    }
  }

  def handleRawRequestWithSchemaBuilder(projectId: String, rawRequest: RawRequest)(schemaBuilderFn: Project => Schema[ApiUserContext, Unit]) = {
    handleRawRequest(projectId, rawRequest) { project =>
      for {
        graphQlRequest <- rawRequest.toGraphQlRequest(project, schema = schemaBuilderFn(project)).toFuture
        result         <- handleGraphQlRequest(graphQlRequest)
      } yield result
    }.recoverWith {
      case e: InvalidGraphQlRequest => Future.successful(OK -> JsObject("error" -> JsString(e.underlying.getMessage)))
    }
  }

  def handleRawRequestForImport(projectId: String, rawRequest: RawRequest): Future[(StatusCode, JsValue)] = {
    handleRawRequest(projectId, rawRequest) { project =>
      val importer = new BulkImport(project)
      importer.executeImport(rawRequest.json).map(x => (200, x))
    }
  }

  def handleRawRequestForExport(projectId: String, rawRequest: RawRequest): Future[(StatusCode, JsValue)] = {
    handleRawRequest(projectId, rawRequest) { project =>
      val resolver = DataResolver(project = project)
      val exporter = new BulkExport(project)
      exporter.executeExport(resolver, rawRequest.json).map(x => (200, x))
    }
  }

  def handleRawRequest(
      projectId: String,
      rawRequest: RawRequest,
  )(
      fn: Project => Future[(StatusCode, JsValue)]
  ): Future[(StatusCode, JsValue)] = {
    for {
      projectWithClientId <- fetchProject(projectId)
      _                   <- verifyAuth(projectWithClientId.project, rawRequest)
      result              <- fn(projectWithClientId.project)
    } yield result
  }

  def verifyAuth(project: Project, rawRequest: RawRequest): Future[Unit] = {
    val authResult = auth.verify(project.secrets, rawRequest.authorizationHeader)
    if (authResult.isSuccess) Future.unit else Future.failed(InvalidToken())
  }

  def handleGraphQlRequest(graphQlRequest: GraphQlRequest): Future[(StatusCode, JsValue)] = {
    graphQlRequestHandler.handle(graphQlRequest)
  }

  def fetchProject(projectId: String): Future[ProjectWithClientId] = {
    val result = projectFetcher.fetch(projectIdOrAlias = projectId)

    result.onComplete {
      case Failure(t) => reporter.report(t, ProjectMetadata(projectId))
      case _          =>
    }

    result map {
      case None         => throw APIErrors.ProjectNotFound(projectId)
      case Some(schema) => schema
    }
  }
}
