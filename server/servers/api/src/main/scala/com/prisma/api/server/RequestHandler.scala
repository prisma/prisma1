package com.prisma.api.server

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model._
import com.prisma.api.ApiDependencies
import com.prisma.api.import_export.{BulkExport, BulkImport}
import com.prisma.api.project.ProjectFetcher
import com.prisma.api.schema.APIErrors.InvalidToken
import com.prisma.api.schema._
import com.prisma.auth.Auth
import com.prisma.client.server.GraphQlRequestHandler
import com.prisma.errors.{ErrorReporter, ProjectMetadata}
import com.prisma.shared.models.{Project, ProjectWithClientId}
import com.prisma.util.json.PlaySprayConversions
import com.prisma.utils.`try`.TryExtensions._
import sangria.schema.Schema
import spray.json.{JsArray, JsNumber, JsObject, JsString, JsValue => SprayJsValue}
import play.api.libs.json.{JsValue => PlayJsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

case class RequestHandler(
    projectFetcher: ProjectFetcher,
    schemaBuilder: SchemaBuilder,
    graphQlRequestHandler: GraphQlRequestHandler,
    auth: Auth,
    log: Function[String, Unit]
)(implicit reporter: ErrorReporter, ec: ExecutionContext, apiDependencies: ApiDependencies)
    extends PlaySprayConversions {

  def handleRawRequestForPublicApi(
      projectId: String,
      rawRequest: RawRequest
  ): Future[(StatusCode, SprayJsValue)] = {
    handleRawRequestWithSchemaBuilder(projectId, rawRequest) { project =>
      schemaBuilder(project)
    }
  }

  def handleRawRequestForPrivateApi(projectId: String, rawRequest: RawRequest): Future[(StatusCode, SprayJsValue)] = {
    handleRawRequestWithSchemaBuilder(projectId, rawRequest) { project =>
      PrivateSchemaBuilder(project)(apiDependencies, apiDependencies.system).build()
    }
  }

  def handleRawRequestWithSchemaBuilder(projectId: String, rawRequest: RawRequest)(schemaBuilderFn: Project => Schema[ApiUserContext, Unit]) = {
    handleRawRequestSprayJson(projectId, rawRequest) { project =>
      for {
        graphQlRequest <- rawRequest.toGraphQlRequest(project, schema = schemaBuilderFn(project)).toFuture
        result         <- handleGraphQlRequest(graphQlRequest)
      } yield result
    }.recoverWith {
      case e: InvalidGraphQlRequest => Future.successful(OK -> JsonErrorHelper.errorJson(rawRequest.id, e.underlying.getMessage))
    }
  }

  def handleRawRequestForImport(projectId: String, rawRequest: RawRequest): Future[(StatusCode, PlayJsValue)] = {
    handleRawRequestPlayJson(projectId, rawRequest) { project =>
      val importer = new BulkImport(project)
      importer.executeImport(rawRequest.json).map(x => (200, x))
    }
  }

  def handleRawRequestForExport(projectId: String, rawRequest: RawRequest): Future[(StatusCode, PlayJsValue)] = {
    handleRawRequestPlayJson(projectId, rawRequest) { project =>
      val resolver = apiDependencies.dataResolver(project)
      val exporter = new BulkExport(project)
      exporter.executeExport(resolver, rawRequest.json).map(x => (200, x))
    }
  }

  def handleRawRequestSprayJson(
      projectId: String,
      rawRequest: RawRequest,
  )(
      fn: Project => Future[(StatusCode, SprayJsValue)]
  ): Future[(StatusCode, SprayJsValue)] = {
    handleRawRequest(projectId, rawRequest)(fn)
  }

  def handleRawRequestPlayJson(
      projectId: String,
      rawRequest: RawRequest,
  )(
      fn: Project => Future[(StatusCode, PlayJsValue)]
  ): Future[(StatusCode, PlayJsValue)] = {
    handleRawRequest(projectId, rawRequest)(fn)
  }

  def handleRawRequest[T](
      projectId: String,
      rawRequest: RawRequest,
  )(
      fn: Project => Future[(StatusCode, T)]
  ): Future[(StatusCode, T)] = {
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

  def handleGraphQlRequest(graphQlRequest: GraphQlRequest): Future[(StatusCode, SprayJsValue)] = {
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
