package cool.graph.client.server

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model._
import cool.graph.bugsnag.{BugSnagger, GraphCoolRequest}
import cool.graph.client.UserContext
import cool.graph.client.authorization.ClientAuth
import cool.graph.client.finder.ProjectFetcher
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.errors.UserAPIErrors.InsufficientPermissions
import cool.graph.shared.logging.RequestLogger
import cool.graph.shared.models.{AuthenticatedRequest, Project, ProjectWithClientId}
import cool.graph.shared.queryPermissions.PermissionSchemaResolver
import cool.graph.util.ErrorHandlerFactory
import cool.graph.utils.`try`.TryExtensions._
import cool.graph.utils.future.FutureUtils.FutureExtensions
import sangria.schema.Schema
import scaldi.Injector
import spray.json.{JsObject, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class RequestHandler(
    errorHandlerFactory: ErrorHandlerFactory,
    projectSchemaFetcher: ProjectFetcher,
    projectSchemaBuilder: ProjectSchemaBuilder,
    graphQlRequestHandler: GraphQlRequestHandler,
    clientAuth: ClientAuth,
    log: Function[String, Unit]
)(implicit
  bugsnagger: BugSnagger,
  inj: Injector,
  ec: ExecutionContext) {

  def handleIntrospectionQuery(projectId: String, requestLogger: RequestLogger): Future[JsValue] = {
    for {
      project <- fetchProject(projectId)
      schema  = projectSchemaBuilder.build(project.project)
      introspectionQueryHandler = IntrospectionQueryHandler(
        project = project.project,
        schema = schema,
        onFailureCallback = onFailureCallback(requestLogger.requestId, project),
        log = log
      )
      resultFuture = introspectionQueryHandler.handle(requestId = requestLogger.requestId, requestIp = "not-used", clientId = project.clientId)
      _            = resultFuture.onComplete(_ => requestLogger.end(Some(project.project.id), Some(project.clientId)))
      result       <- resultFuture
    } yield result
  }

  def onFailureCallback(requestId: String, project: ProjectWithClientId): PartialFunction[Throwable, Any] = {
    case t: Throwable =>
      val request = GraphCoolRequest(
        requestId = requestId,
        clientId = Some(project.clientId),
        projectId = Some(project.project.id),
        query = "",
        variables = ""
      )

      bugsnagger.report(t, request)
  }

  def handleRawRequestForPermissionSchema(
      projectId: String,
      rawRequest: RawRequest
  ): Future[(StatusCode, JsValue)] = {
    def checkIfUserMayQueryPermissionSchema(auth: Option[AuthenticatedRequest]): Unit = {
      val mayQueryPermissionSchema = auth.exists(_.isAdmin)
      if (!mayQueryPermissionSchema) {
        throw InsufficientPermissions("Insufficient permissions for this query")
      }
    }

    handleRawRequest(
      projectId = projectId,
      rawRequest = rawRequest,
      schemaFn = PermissionSchemaResolver.permissionSchema,
      checkAuthFn = checkIfUserMayQueryPermissionSchema
    )
  }

  def handleRawRequestForProjectSchema(
      projectId: String,
      rawRequest: RawRequest
  ): Future[(StatusCode, JsValue)] = handleRawRequest(projectId, rawRequest, projectSchemaBuilder.build)

  def handleRawRequest(
      projectId: String,
      rawRequest: RawRequest,
      schemaFn: Project => Schema[UserContext, Unit],
      checkAuthFn: Option[AuthenticatedRequest] => Unit = _ => ()
  ): Future[(StatusCode, JsValue)] = {
    val graphQlRequestFuture = for {
      projectWithClientId  <- fetchProject(projectId)
      authenticatedRequest <- getAuthContext(projectWithClientId, rawRequest.authorizationHeader)
      _                    = checkAuthFn(authenticatedRequest)
      schema               = schemaFn(projectWithClientId.project)
      graphQlRequest       <- rawRequest.toGraphQlRequest(authenticatedRequest, projectWithClientId, schema).toFuture
    } yield graphQlRequest

    graphQlRequestFuture.toFutureTry.flatMap {
      case Success(graphQlRequest) =>
        handleGraphQlRequest(graphQlRequest)

      case Failure(e: InvalidGraphQlRequest) =>
        Future.successful(OK -> JsObject("error" -> JsString(e.underlying.getMessage)))

      case Failure(e) =>
        val unhandledErrorLogger = errorHandlerFactory.unhandledErrorHandler(
          requestId = rawRequest.id,
          query = rawRequest.json.toString,
          projectId = Some(projectId)
        )
        Future.successful(unhandledErrorLogger(e))
    }
  }

  def handleGraphQlRequest(graphQlRequest: GraphQlRequest): Future[(StatusCode, JsValue)] = {
    val resultFuture = graphQlRequestHandler.handle(graphQlRequest)
    resultFuture.onComplete(_ => graphQlRequest.logger.end(Some(graphQlRequest.project.id), Some(graphQlRequest.projectWithClientId.clientId)))

    resultFuture.recover {
      case error: Throwable =>
        val unhandledErrorLogger = errorHandlerFactory.unhandledErrorHandler(
          requestId = graphQlRequest.id,
          query = graphQlRequest.json.toString,
          clientId = Some(graphQlRequest.projectWithClientId.clientId),
          projectId = Some(graphQlRequest.projectWithClientId.id)
        )
        unhandledErrorLogger(error)
    }
  }

  def fetchProject(projectId: String): Future[ProjectWithClientId] = {
    val result = projectSchemaFetcher.fetch(projectIdOrAlias = projectId)

    result.onFailure {
      case t =>
        val request = GraphCoolRequest(requestId = "", clientId = None, projectId = Some(projectId), query = "", variables = "")
        bugsnagger.report(t, request)
    }

    result map {
      case None         => throw UserAPIErrors.ProjectNotFound(projectId)
      case Some(schema) => schema
    }
  }

  private def getAuthContext(
      projectWithClientId: ProjectWithClientId,
      authorizationHeader: Option[String]
  ): Future[Option[AuthenticatedRequest]] = {

    authorizationHeader match {
      case Some(header) if header.startsWith("Bearer") =>
//        ToDo
//        The validation is correct but the error message that the token is valid, but user is not a collaborator seems off
//        For now revert to the old state of returning None for a failed Auth Token and no error
//        val res = ClientAuth()
//          .authenticateRequest(header.stripPrefix("Bearer "), projectWithClientId.project)
//          .toFutureTry
//
//        res.flatMap {
//          case Failure(e: Exception)            => Future.failed(InvalidGraphQlRequest(e))
//          case Success(a: AuthenticatedRequest) => Future.successful(Some(a))
//          case _                                => Future.successful(None)
//        }

        clientAuth
          .authenticateRequest(header.stripPrefix("Bearer "), projectWithClientId.project)
          .toFutureTry
          .map(_.toOption)
      case _ =>
        Future.successful(None)
    }
  }
}
