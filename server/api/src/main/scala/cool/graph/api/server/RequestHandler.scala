package cool.graph.api.server

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model._
import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataResolver
import cool.graph.api.database.import_export.{BulkExport, BulkImport}
import cool.graph.api.project.ProjectFetcher
import cool.graph.api.schema.{APIErrors, SchemaBuilder}
import cool.graph.bugsnag.{BugSnagger, GraphCoolRequest}
import cool.graph.client.server.GraphQlRequestHandler
import cool.graph.shared.models.ProjectWithClientId
import cool.graph.utils.`try`.TryExtensions._
import cool.graph.utils.future.FutureUtils.FutureExtensions
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

  def handleRawRequest(
      projectId: String,
      rawRequest: RawRequest
  ): Future[(StatusCode, JsValue)] = {
    val graphQlRequestFuture = for {
      projectWithClientId <- fetchProject(projectId)
      schema              = schemaBuilder(projectWithClientId.project)
      _                   <- auth.verify(projectWithClientId.project, rawRequest.authorizationHeader).toFuture
      graphQlRequest      <- rawRequest.toGraphQlRequest(projectWithClientId, schema).toFuture
    } yield graphQlRequest

    graphQlRequestFuture.toFutureTry.flatMap {
      case Success(graphQlRequest) =>
        handleGraphQlRequest(graphQlRequest)

      case Failure(e: InvalidGraphQlRequest) =>
        Future.successful(OK -> JsObject("error" -> JsString(e.underlying.getMessage)))

      case Failure(e) =>
        Future.successful(ErrorHandler(rawRequest.id).handle(e))
    }
  }

  def handleRawRequestForImport(projectId: String, rawRequest: RawRequest): Future[(StatusCode, JsValue)] = {
    val graphQlRequestFuture: Future[Future[JsValue]] = for {
      projectWithClientId <- fetchProject(projectId)
      importer            = new BulkImport(projectWithClientId.project)
      res                 = importer.executeImport(rawRequest.json)
    } yield res

    val response: Future[JsValue] = graphQlRequestFuture.flatMap(identity)

    response.map(x => (200, x))
  }

  def handleRawRequestForExport(projectId: String, rawRequest: RawRequest): Future[(StatusCode, JsValue)] = {

    val graphQlRequestFuture: Future[Future[JsValue]] = for {
      projectWithClientId <- fetchProject(projectId)
      resolver            = DataResolver(project = projectWithClientId.project)
      exporter            = new BulkExport(projectWithClientId.project)
      res                 = exporter.executeExport(resolver, rawRequest.json)
    } yield res
    import spray.json._

    val response: Future[JsValue] = graphQlRequestFuture.flatMap(identity)

    response.map(x => (200, x))
  }

  def handleGraphQlRequest(graphQlRequest: GraphQlRequest): Future[(StatusCode, JsValue)] = {
    val resultFuture = graphQlRequestHandler.handle(graphQlRequest)

    resultFuture.recover {
      case error: Throwable =>
        ErrorHandler(graphQlRequest.id).handle(error)
    }
  }

  def fetchProject(projectId: String): Future[ProjectWithClientId] = {
    val result = projectFetcher.fetch(projectIdOrAlias = projectId)

    result.onFailure {
      case t =>
        val request = GraphCoolRequest(requestId = "", clientId = None, projectId = Some(projectId), query = "", variables = "")
        bugsnagger.report(t, request)
    }

    result map {
      case None         => throw APIErrors.ProjectNotFound(projectId)
      case Some(schema) => schema
    }
  }
}
