package com.prisma.image

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.prisma.akkautil.throttler.Throttler
import com.prisma.akkautil.throttler.Throttler.ThrottleBufferFullException
import com.prisma.api.schema.CommonErrors.ThrottlerBufferFull
import com.prisma.api.server.{RawRequest => LegacyRawRequest}
import com.prisma.api.{ApiDependencies, ApiMetrics}
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.schema.{DeployApiError, SystemUserContext}
import com.prisma.sangria.utils.ErrorHandler
import com.prisma.sangria_server._
import com.prisma.shared.models.ConnectorCapability.ImportExportCapability
import com.prisma.shared.models.ProjectId
import com.prisma.subscriptions.SubscriptionDependencies
import com.prisma.util.env.EnvUtils
import com.prisma.websocket.WebsocketServer
import com.prisma.workers.WorkerServer
import com.prisma.workers.dependencies.WorkerDependencies
import play.api.libs.json.{JsValue, Json}
import sangria.execution.{Executor, QueryAnalysisError}

import scala.concurrent.{ExecutionContext, Future}

case class SangriaHandlerImpl(
    managementApiEnabled: Boolean
)(
    implicit system: ActorSystem,
    materializer: ActorMaterializer,
    deployDependencies: DeployDependencies,
    apiDependencies: ApiDependencies,
    subscriptionDependencies: SubscriptionDependencies,
    workerDependencies: WorkerDependencies
) extends SangriaHandler {
  import com.prisma.utils.future.FutureUtils._
  import system.dispatcher

  import scala.concurrent.duration._

  val logSlowQueries        = EnvUtils.asBoolean("SLOW_QUERIES_LOGGING").getOrElse(false)
  val slowQueryLogThreshold = EnvUtils.asInt("SLOW_QUERIES_LOGGING_THRESHOLD").getOrElse(1000)
  val projectIdEncoder      = apiDependencies.projectIdEncoder
  val websocketServer       = WebsocketServer(subscriptionDependencies)
  val workerServer          = WorkerServer(workerDependencies)

  lazy val unthrottledProjectIds = sys.env.get("UNTHROTTLED_PROJECT_IDS") match {
    case Some(envValue) => envValue.split('|').filter(_.nonEmpty).toVector.map(projectIdEncoder.fromEncodedString)
    case None           => Vector.empty
  }

  lazy val throttler: Option[Throttler[ProjectId]] = {
    for {
      throttlingRate    <- EnvUtils.asInt("THROTTLING_RATE")
      maxCallsInFlights <- EnvUtils.asInt("THROTTLING_MAX_CALLS_IN_FLIGHT")
    } yield {
      val per = EnvUtils.asInt("THROTTLING_RATE_PER_SECONDS").getOrElse(1)
      Throttler[ProjectId](
        groupBy = pid => pid.name + "~" + pid.stage,
        amount = throttlingRate,
        per = per.seconds,
        timeout = 25.seconds,
        maxCallsInFlight = maxCallsInFlights.toInt
      )
    }
  }

  override def onStart() = {
    if (managementApiEnabled) {
      deployDependencies.migrator.initialize
    }
    workerServer.onStart.map(_ => ())
  }

  override def handleRawRequest(rawRequest: RawRequest)(implicit ec: ExecutionContext) = {
    val (projectSegments, reservedSegment) = splitReservedSegment(rawRequest.path.toList)
    val projectId                          = projectIdEncoder.fromSegments(projectSegments)
    val projectIdAsString                  = projectIdEncoder.toEncodedString(projectId)
    reservedSegment match {
      case Some("import") =>
        if (apiDependencies.apiConnector.hasCapability(ImportExportCapability)) {
          val result = apiDependencies.requestHandler.handleRawRequestForImport(projectId = projectIdAsString, rawRequest = rawRequest.toLegacy)
          result.onComplete(_ => logRequestEnd(rawRequest, projectIdAsString))
          result.map(_._2)
        } else {
          sys.error(s"The connector is missing the import / export capability.")
        }

      case Some("export") =>
        if (apiDependencies.apiConnector.hasCapability(ImportExportCapability)) {
          val result = apiDependencies.requestHandler.handleRawRequestForExport(projectId = projectIdAsString, rawRequest = rawRequest.toLegacy)
          result.onComplete(_ => logRequestEnd(rawRequest, projectIdAsString))
          result.map(_._2)
        } else {
          sys.error(s"The connector is missing the import / export capability.")
        }

      case _ =>
        super.handleRawRequest(rawRequest)
    }
  }

  override def handleGraphQlQuery(request: RawRequest, query: GraphQlQuery)(implicit ec: ExecutionContext): Future[JsValue] = {
    if (request.path == Vector("management") && managementApiEnabled) {
      handleQueryForManagementApi(request, query)
    } else {
      handleQueryForServiceApi(request, query)
    }
  }

  override def supportedWebsocketProtocols = websocketServer.supportedProtocols

  override def newWebsocketSession(request: RawWebsocketRequest) = {
    val projectId          = projectIdEncoder.toEncodedString(projectIdEncoder.fromSegments(request.path.toList))
    val isV7               = request.protocol == websocketServer.v7ProtocolName
    val originalFlow       = websocketServer.newSession(projectId, isV7)
    val sangriaHandlerFlow = Flow[WebSocketMessage].map(modelToAkkaWebsocketMessage).via(originalFlow).map(akkaWebSocketMessageToModel)
    sangriaHandlerFlow
  }

  private def modelToAkkaWebsocketMessage(message: WebSocketMessage): Message = TextMessage(message.body)
  private def akkaWebSocketMessageToModel(message: Message) = {
    message match {
      case TextMessage.Strict(body) => WebSocketMessage(body)
      case x                        => sys.error(s"Not supported: $x")
    }
  }

  private def handleQueryForManagementApi(request: RawRequest, query: GraphQlQuery): Future[JsValue] = {
    import com.prisma.deploy.server.JsonMarshalling._
    def errorExtractor(t: Throwable): Option[Int] = t match {
      case e: DeployApiError => Some(e.code)
      case _                 => None
    }
    val userContext = SystemUserContext(authorizationHeader = request.headers.get("Authorization"))
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
    reservedSegment match {
      case None =>
        throttleApiCallIfNeeded(projectId, rawRequest)

      case Some("private") =>
        val result = apiDependencies.requestHandler.handleRawRequestForPrivateApi(projectId = projectIdAsString, rawRequest = rawRequest.toLegacy)
        result.onComplete(_ => logRequestEnd(rawRequest, projectIdAsString))
        result.map(_._2)

      case Some(x) =>
        sys.error(s"cannot happen: $x")
    }
  }

  def splitReservedSegment(elements: List[String]): (List[String], Option[String]) = {
    val reservedSegments = Set("private", "import", "export")
    if (elements.nonEmpty && reservedSegments.contains(elements.last)) {
      (elements.dropRight(1), elements.lastOption)
    } else {
      (elements, None)
    }
  }

  def throttleApiCallIfNeeded(projectId: ProjectId, rawRequest: RawRequest) = {
    throttler match {
      case Some(throttler) if !unthrottledProjectIds.contains(projectId) => throttledCall(projectId, rawRequest, throttler)
      case _                                                             => handleRequestForPublicApi(projectId, rawRequest)
    }
  }

  def throttledCall(projectId: ProjectId, rawRequest: RawRequest, throttler: Throttler[ProjectId]) = {
    val result = throttler.throttled(projectId) { () =>
      handleRequestForPublicApi(projectId, rawRequest)
    }
    result.toFutureTry.map {
      case scala.util.Success(result) =>
        // TODO: do we really need this?
//        respondWithHeader(RawHeader("Throttled-By", result.throttledBy.toString + "ms")) {
//          complete(result.result)
//        }
        result.result

      case scala.util.Failure(_: ThrottleBufferFullException) =>
        throw ThrottlerBufferFull()

      case scala.util.Failure(exception) =>
        throw exception
    }
  }

  def handleRequestForPublicApi(projectId: ProjectId, rawRequest: RawRequest) = {
    val result = apiDependencies.requestHandler.handleRawRequestForPublicApi(projectIdEncoder.toEncodedString(projectId), rawRequest.toLegacy)
    result.onComplete { _ =>
      logRequestEndAndQueryIfEnabled(rawRequest, projectIdEncoder.toEncodedString(projectId), rawRequest.json)
    }
    result.map(_._2)
  }

  def logRequestEndAndQueryIfEnabled(rawRequest: RawRequest, projectId: String, json: JsValue, throttledBy: Long = 0) = {
    val actualDuration = logRequestEnd(rawRequest, projectId, throttledBy)

    if (logSlowQueries && actualDuration > slowQueryLogThreshold) {
      println("SLOW QUERY - DURATION: " + actualDuration)
      println("QUERY: " + json)
    }
  }

  def logRequestEnd(rawRequest: RawRequest, projectId: String, throttledBy: Long = 0) = {
    val end            = System.currentTimeMillis()
    val actualDuration = end - rawRequest.timestampInMillis - throttledBy
    val metricKey      = metricKeyFor(projectId)

    ApiMetrics.requestDuration.record(actualDuration, Seq(metricKey))
    ApiMetrics.requestCounter.inc(metricKey)
    actualDuration
  }

  def metricKeyFor(projectId: String): String = projectId.replace(projectIdEncoder.stageSeparator, '-').replace(projectIdEncoder.workspaceSeparator, '-')

  implicit class RawRequestExtensions(rawRequest: RawRequest) {
    def toLegacy: LegacyRawRequest = {
      LegacyRawRequest(rawRequest.id, rawRequest.json, rawRequest.ip, rawRequest.headers.get("Authorization"))
    }
  }
}
