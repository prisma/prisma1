package com.prisma.image

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.import_export.{BulkExport, BulkImport}
import com.prisma.api.schema.APIErrors.AuthFailure
import com.prisma.api.schema.{PrivateSchemaBuilder, UserFacingError}
import com.prisma.api.{ApiDependencies, ApiMetrics}
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.schema.{DeployApiError, SystemUserContext}
import com.prisma.sangria.utils.ErrorHandler
import com.prisma.sangria_server._
import com.prisma.shared.models.ConnectorCapability.ImportExportCapability
import com.prisma.shared.models.Project
import com.prisma.subscriptions.SubscriptionDependencies
import com.prisma.util.env.EnvUtils
import com.prisma.websocket.WebSocketHandler
import com.prisma.workers.WorkerServer
import com.prisma.workers.dependencies.WorkerDependencies
import play.api.libs.json.{JsValue, Json}
import sangria.execution.{Executor, QueryAnalysisError}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class SangriaHandlerImpl(managementApiEnabled: Boolean)(
    implicit system: ActorSystem,
    materializer: ActorMaterializer,
    deployDependencies: DeployDependencies,
    apiDependencies: ApiDependencies,
    subscriptionDependencies: SubscriptionDependencies,
    workerDependencies: WorkerDependencies
) extends SangriaHandler {
  import system.dispatcher

  val logSlowQueries        = EnvUtils.asBoolean("SLOW_QUERIES_LOGGING").getOrElse(false)
  val slowQueryLogThreshold = EnvUtils.asInt("SLOW_QUERIES_LOGGING_THRESHOLD").getOrElse(1000)
  val projectIdEncoder      = apiDependencies.projectIdEncoder
  val websocketHandler      = WebSocketHandler(subscriptionDependencies)
  val workerServer          = WorkerServer(workerDependencies)
  val requestThrottler      = RequestThrottler()

  override def onStart() = {
    if (managementApiEnabled) {
      deployDependencies.migrator.initialize
    }
    workerServer.onStart.map(_ => ())
  }

  override def handleRawRequest(rawRequest: RawRequest)(implicit ec: ExecutionContext): Future[Response] = {
    val (projectSegments, reservedSegment) = splitReservedSegment(rawRequest.path.toList)
    val projectId                          = projectIdEncoder.fromSegments(projectSegments)
    val projectIdAsString                  = projectIdEncoder.toEncodedString(projectId)
    val result = reservedSegment match {
      case Some("import") =>
        verifyAuth(projectIdAsString, rawRequest) { project =>
          if (apiDependencies.apiConnector.capabilities.has(ImportExportCapability)) {
            val result = new BulkImport(project).executeImport(rawRequest.json)
            result.onComplete(_ => logRequestEnd(rawRequest, projectIdAsString))
            result.map(Response(_))
          } else {
            sys.error(s"The connector is missing the import / export capability.")
          }
        }

      case Some("export") =>
        verifyAuth(projectIdAsString, rawRequest) { project =>
          if (apiDependencies.apiConnector.capabilities.has(ImportExportCapability)) {
            val resolver = apiDependencies.dataResolver(project)
            val result   = new BulkExport(project).executeExport(resolver, rawRequest.json)
            result.onComplete(_ => logRequestEnd(rawRequest, projectIdAsString))
            result.map(Response(_))
          } else {
            sys.error(s"The connector is missing the import / export capability.")
          }
        }

      case _ =>
        requestThrottler.throttleCallIfNeeded(projectIdAsString, isManagementApiRequest = isManagementApiRequest(rawRequest)) {
          super.handleRawRequest(rawRequest)
        }
    }

    result.recover {
      case e: UserFacingError => Response(JsonErrorHelper.errorJson(rawRequest.id, e.getMessage, e.code))
    }
  }

  override def handleGraphQlQuery(request: RawRequest, query: GraphQlQuery)(implicit ec: ExecutionContext): Future[JsValue] = {
    if (isManagementApiRequest(request) && managementApiEnabled) {
      handleQueryForManagementApi(request, query)
    } else {
      handleQueryForServiceApi(request, query)
    }
  }

  def isManagementApiRequest(request: RawRequest): Boolean = request.path == Vector("management")

  private def verifyAuth[T](projectId: String, rawRequest: RawRequest)(fn: Project => Future[T]): Future[T] = {
    (for {
      project <- apiDependencies.projectFetcher.fetch_!(projectId)
    } yield {
      if (project.secrets.nonEmpty) {
        val token = apiDependencies.auth.extractToken(rawRequest.headers.get("authorization"))
        apiDependencies.auth.verifyToken(token, project.secrets) match {
          case Success(_) => project
          case Failure(_) => throw AuthFailure()
        }
      } else {
        project
      }
    }).flatMap(fn)
  }

  override def supportedWebsocketProtocols                       = websocketHandler.supportedProtocols
  override def newWebsocketSession(request: RawWebsocketRequest) = websocketHandler.newWebsocketSession(request)

  private def handleQueryForManagementApi(request: RawRequest, query: GraphQlQuery): Future[JsValue] = {
    import com.prisma.deploy.server.JsonMarshalling._
    def errorExtractor(t: Throwable): Option[Int] = t match {
      case e: DeployApiError => Some(e.code)
      case _                 => None
    }
    val userContext = SystemUserContext(authorizationHeader = request.headers.get("authorization"))
    val errorHandler =
      ErrorHandler(
        request.id,
        request.method.name,
        request.path.mkString("/"),
        request.headers.toVector,
        query.queryString,
        query.variables,
        deployDependencies.reporter,
        errorCodeExtractor = errorExtractor
      )
    Executor
      .execute(
        schema = deployDependencies.managementSchemaBuilder(userContext),
        queryAst = query.query,
        userContext = userContext,
        variables = query.variables,
        operationName = query.operationName,
        middleware = List.empty,
        exceptionHandler = errorHandler.sangriaExceptionHandler
      )
      .recover {
        case e: QueryAnalysisError =>
          e.resolveError
        case e: DeployApiError =>
          Json.obj("code" -> e.code, "requestId" -> request.id, "error" -> e.getMessage)
      }
  }

  private def handleQueryForServiceApi(rawRequest: RawRequest, query: GraphQlQuery): Future[JsValue] = {
    val (projectSegments, reservedSegment) = splitReservedSegment(rawRequest.path.toList)
    val projectId                          = projectIdEncoder.fromSegments(projectSegments)
    val projectIdAsString                  = projectIdEncoder.toEncodedString(projectId)
    verifyAuth(projectIdAsString, rawRequest) { project =>
      reservedSegment match {
        case None =>
          handleRequestForPublicApi(project, rawRequest, query)

        case Some("private") =>
          val result = handleRequestForPrivateApi(project, rawRequest, query)
          result.onComplete(_ => logRequestEnd(rawRequest, projectIdAsString))
          result

        case Some(x) =>
          sys.error(s"cannot happen: $x")
      }
    }
  }

  private def splitReservedSegment(elements: List[String]): (List[String], Option[String]) = {
    val reservedSegments = Set("private", "import", "export")
    if (elements.nonEmpty && reservedSegments.contains(elements.last)) {
      (elements.dropRight(1), elements.lastOption)
    } else {
      (elements, None)
    }
  }

  private def handleRequestForPublicApi(project: Project, rawRequest: RawRequest, query: GraphQlQuery) = {
    val result = apiDependencies.queryExecutor.execute(
      requestId = rawRequest.id,
      queryString = query.queryString,
      queryAst = query.query,
      variables = query.variables,
      operationName = query.operationName,
      project = project,
      schema = apiDependencies.apiSchemaBuilder(project)
    )
    result.onComplete { _ =>
      logRequestEndAndQueryIfEnabled(rawRequest, project.id, rawRequest.json)
    }
    result
  }

  private def handleRequestForPrivateApi(project: Project, rawRequest: RawRequest, query: GraphQlQuery) = {
    val result = apiDependencies.queryExecutor.execute(
      requestId = rawRequest.id,
      queryString = query.queryString,
      queryAst = query.query,
      variables = query.variables,
      operationName = query.operationName,
      project = project,
      schema = PrivateSchemaBuilder(project).build()
    )
    result.onComplete { _ =>
      logRequestEndAndQueryIfEnabled(rawRequest, project.id, rawRequest.json)
    }
    result
  }

  private def logRequestEndAndQueryIfEnabled(rawRequest: RawRequest, projectId: String, json: JsValue, throttledBy: Long = 0) = {
    val actualDuration = logRequestEnd(rawRequest, projectId, throttledBy)

    if (logSlowQueries && actualDuration > slowQueryLogThreshold) {
      println("SLOW QUERY - DURATION: " + actualDuration)
      println("QUERY: " + json)
    }
  }

  private def logRequestEnd(rawRequest: RawRequest, projectId: String, throttledBy: Long = 0) = {
    val end            = System.currentTimeMillis()
    val actualDuration = end - rawRequest.timestampInMillis - throttledBy
    val metricKey      = metricKeyFor(projectId)

    ApiMetrics.requestDuration.record(actualDuration, Seq(metricKey))
    ApiMetrics.requestCounter.inc(metricKey)
    actualDuration
  }

  private def metricKeyFor(projectId: String): String = {
    projectId.replace(projectIdEncoder.stageSeparator, '-').replace(projectIdEncoder.workspaceSeparator, '-')
  }
}
