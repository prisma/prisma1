package cool.graph.api.server

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model._
import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataResolver
import cool.graph.api.database.import_export.{BulkExport, BulkImport}
import cool.graph.api.project.ProjectFetcher
import cool.graph.api.schema.{APIErrors, ApiUserContext, PrivateSchemaBuilder, SchemaBuilder}
import cool.graph.bugsnag.{BugSnagger, GraphCoolRequest}
import cool.graph.client.server.GraphQlRequestHandler
import cool.graph.shared.models.{Project, ProjectWithClientId}
import cool.graph.utils.`try`.TryExtensions._
import cool.graph.utils.future.FutureUtils.FutureExtensions
import sangria.schema.Schema
import spray.json.{JsObject, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class RequestHandler(
    projectFetcher: ProjectFetcher,
    schemaBuilder: SchemaBuilder,
    graphQlRequestHandler: GraphQlRequestHandler,
    auth: Auth,
    log: Function[String, Unit]
)(implicit bugsnagger: BugSnagger, ec: ExecutionContext, apiDependencies: ApiDependencies) {

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

  def handleRawRequestWithSchemaBuilder(
      projectId: String,
      rawRequest: RawRequest
  )(
      schemaBuilderFn: Project => Schema[ApiUserContext, Unit]
  ) = {
    handleRawRequest(projectId, rawRequest) { project =>
      for {
        graphQlRequest <- rawRequest.toGraphQlRequest(project, schema = schemaBuilderFn(project)).toFuture
        result         <- handleGraphQlRequest(graphQlRequest)
      } yield result
    }.recoverWith {
      case e: InvalidGraphQlRequest => Future.successful(OK -> JsObject("error" -> JsString(e.underlying.getMessage)))
      case exception                => Future.successful(ErrorHandler(rawRequest.id).handle(exception))
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
  )(fn: Project => Future[(StatusCode, JsValue)]): Future[(StatusCode, JsValue)] = {
    for {
      projectWithClientId <- fetchProject(projectId)
      _                   <- auth.verify(projectWithClientId.project, rawRequest.authorizationHeader).toFuture
      result              <- fn(projectWithClientId.project)
    } yield result
  }

  def handleGraphQlRequest(graphQlRequest: GraphQlRequest): Future[(StatusCode, JsValue)] = {
    val resultFuture = graphQlRequestHandler.handle(graphQlRequest)

    resultFuture.recover { case error: Throwable => ErrorHandler(graphQlRequest.id).handle(error) }
  }

  def fetchProject(projectId: String): Future[ProjectWithClientId] = {
    val result = projectFetcher.fetch(projectIdOrAlias = projectId)

    result.onComplete {
      case Failure(t) =>
        val request = GraphCoolRequest(requestId = "", clientId = None, projectId = Some(projectId), query = "", variables = "")
        bugsnagger.report(t, request)

      case _ =>
    }

    result map {
      case None         => throw APIErrors.ProjectNotFound(projectId)
      case Some(schema) => schema
    }
  }
}
