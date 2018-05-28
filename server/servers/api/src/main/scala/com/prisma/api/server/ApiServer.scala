package com.prisma.api.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.Server
import com.prisma.akkautil.throttler.Throttler
import com.prisma.akkautil.throttler.Throttler.ThrottleBufferFullException
import com.prisma.api.schema.APIErrors.ProjectNotFound
import com.prisma.api.schema.CommonErrors.ThrottlerBufferFullException
import com.prisma.api.schema.{SchemaBuilder, UserFacingError}
import com.prisma.api.{ApiDependencies, ApiMetrics}
import com.prisma.metrics.extensions.TimeResponseDirectiveImpl
import com.prisma.shared.models.{ProjectId, ProjectWithClientId}
import com.prisma.util.env.EnvUtils
import com.typesafe.scalalogging.LazyLogging
import cool.graph.cuid.Cuid.createCuid
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json._

import scala.concurrent.Future
import scala.language.postfixOps

case class ApiServer(
    schemaBuilder: SchemaBuilder,
    prefix: String = ""
)(
    implicit apiDependencies: ApiDependencies,
    system: ActorSystem,
    materializer: ActorMaterializer
) extends Server
    with PlayJsonSupport
    with LazyLogging {
  import system.dispatcher

  val log: String => Unit = (msg: String) => logger.info(msg)
  val requestPrefix       = sys.env.getOrElse("ENV", "local")
  val projectFetcher      = apiDependencies.projectFetcher
  val reservedSegments    = Set("private", "import", "export")
  val projectIdEncoder    = apiDependencies.projectIdEncoder

  import scala.concurrent.duration._

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

  val innerRoutes = extractRequest { _ =>
    val requestId            = requestPrefix + ":api:" + createCuid()
    val requestBeginningTime = System.currentTimeMillis()

    def logRequestEnd(projectId: String, throttledBy: Long = 0) = {
      val end            = System.currentTimeMillis()
      val actualDuration = end - requestBeginningTime - throttledBy
      val metricKey      = metricKeyFor(projectId)

      ApiMetrics.requestDuration.record(actualDuration, Seq(metricKey))
      ApiMetrics.requestCounter.inc(metricKey)

//      log(
//        Json
//          .toJson(
//            LogData(
//              key = LogKey.RequestComplete,
//              requestId = requestId,
//              projectId = Some(projectId),
//              payload = Some(
//                Map(
//                  "request_duration" -> (end - requestBeginningTime),
//                  "throttled_by"     -> throttledBy
//                ))
//            )
//          )
//          .toString())
    }

    def throttleApiCallIfNeeded(projectId: ProjectId, rawRequest: RawRequest) = {
      throttler match {
        case Some(throttler) if !unthrottledProjectIds.contains(projectId) => throttledCall(projectId, rawRequest, throttler)
        case _                                                             => unthrottledCall(projectId, rawRequest)
      }
    }

    def unthrottledCall(projectId: ProjectId, rawRequest: RawRequest) = {
      val result = handleRequestForPublicApi(projectId, rawRequest)
      complete(result)
    }

    def throttledCall(projectId: ProjectId, rawRequest: RawRequest, throttler: Throttler[ProjectId]) = {
      val result = throttler.throttled(projectId) { () =>
        handleRequestForPublicApi(projectId, rawRequest)
      }
      onComplete(result) {
        case scala.util.Success(result) =>
          respondWithHeader(RawHeader("Throttled-By", result.throttledBy.toString + "ms")) {
            complete(result.result)
          }

        case scala.util.Failure(_: ThrottleBufferFullException) =>
          throw ThrottlerBufferFullException()

        case scala.util.Failure(exception) =>
          throw exception
      }
    }

    def handleRequestForPublicApi(projectId: ProjectId, rawRequest: RawRequest) = {
      val result = apiDependencies.requestHandler.handleRawRequestForPublicApi(projectIdEncoder.toEncodedString(projectId), rawRequest)
      result.onComplete { res =>
        logRequestEnd(projectIdEncoder.toEncodedString(projectId))
      }
      result

    }

    pathPrefix(Segments(min = 0, max = 4)) { segments =>
      post {
        val (projectSegments, reservedSegment) = splitReservedSegment(segments)
        val projectId                          = projectIdEncoder.fromSegments(projectSegments)
        val projectIdAsString                  = projectIdEncoder.toEncodedString(projectId)

        handleExceptions(toplevelExceptionHandler(requestId)) {
          extractRawRequest(requestId) { rawRequest =>
            reservedSegment match {
              case None =>
                throttleApiCallIfNeeded(projectId, rawRequest)

              case Some("private") =>
                val result = apiDependencies.requestHandler.handleRawRequestForPrivateApi(projectId = projectIdAsString, rawRequest = rawRequest)
                result.onComplete(_ => logRequestEnd(projectIdAsString))
                complete(result)

              case Some("import") =>
                withRequestTimeout(5.minutes) {
                  val result = apiDependencies.requestHandler.handleRawRequestForImport(projectId = projectIdAsString, rawRequest = rawRequest)
                  result.onComplete(_ => logRequestEnd(projectIdAsString))
                  complete(result)
                }

              case Some("export") =>
                withRequestTimeout(5.minutes) {
                  val result = apiDependencies.requestHandler.handleRawRequestForExport(projectId = projectIdAsString, rawRequest = rawRequest)
                  result.onComplete(_ => logRequestEnd(projectIdAsString))
                  complete(result)
                }

              case Some(x) =>
                complete(StatusCodes.BadRequest, s"Invalid path segment $x")
            }
          }
        }
      } ~ get {
        getFromResource("graphiql.html")
      }
    }
  }

  def extractRawRequest(requestId: String)(fn: RawRequest => Route): Route = {
    optionalHeaderValueByName("Authorization") { authorizationHeader =>
      TimeResponseDirectiveImpl(ApiMetrics).timeResponse {
        entity(as[JsValue]) { requestJson =>
          extractClientIP { clientIp =>
            respondWithHeader(RawHeader("Request-Id", requestId)) {
              fn(
                RawRequest(
                  id = requestId,
                  json = requestJson,
                  ip = clientIp.toString,
                  authorizationHeader = authorizationHeader
                )
              )
            }
          }
        }
      }
    }
  }

  def splitReservedSegment(elements: List[String]): (List[String], Option[String]) = {
    if (elements.nonEmpty && reservedSegments.contains(elements.last)) {
      (elements.dropRight(1), elements.lastOption)
    } else {
      (elements, None)
    }
  }

  def fetchProject(projectId: String): Future[ProjectWithClientId] = {
    val result = projectFetcher.fetch(projectIdOrAlias = projectId)

    result map {
      case None         => throw ProjectNotFound(projectId)
      case Some(schema) => schema
    }
  }

  def toplevelExceptionHandler(requestId: String) = ExceptionHandler {
    case e: UserFacingError =>
      complete(OK -> JsonErrorHelper.errorJson(requestId, e.getMessage, e.code))

    case e: Throwable =>
      println(e.getMessage)
      e.printStackTrace()
      apiDependencies.reporter.report(e)
      complete(InternalServerError -> JsonErrorHelper.errorJson(requestId, e.getMessage))
  }

  def metricKeyFor(projectId: String): String = projectId.replace(projectIdEncoder.stageSeparator, '-').replace(projectIdEncoder.workspaceSeparator, '-')
}
