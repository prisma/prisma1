package cool.graph.schemamanager

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.{BadRequest, OK, Unauthorized}
import akka.http.scaladsl.server.Directives.{complete, get, handleExceptions, optionalHeaderValueByName, parameters, pathPrefix, _}
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import cool.graph.akkautil.http.Server
import cool.graph.bugsnag.BugSnagger
import cool.graph.shared.SchemaSerializer
import cool.graph.shared.errors.SystemErrors.InvalidProjectId
import cool.graph.shared.logging.RequestLogger
import cool.graph.shared.models.ProjectWithClientId
import cool.graph.system.database.finder.{CachedProjectResolver, ProjectResolver}
import cool.graph.util.ErrorHandlerFactory
import scaldi.{Injectable, Injector}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.Future

case class SchemaManagerServer(prefix: String = "")(
    implicit system: ActorSystem,
    materializer: ActorMaterializer,
    bugsnag: BugSnagger,
    inj: Injector
) extends Server
    with Injectable
    with LazyLogging {
  import system.dispatcher

  val config                  = inject[Config](identified by "config")
  val internalDatabase        = inject[DatabaseDef](identified by "internal-db")
  val cachedProjectResolver   = inject[CachedProjectResolver](identified by "cachedProjectResolver")
  val uncachedProjectResolver = inject[ProjectResolver](identified by "uncachedProjectResolver")
  val schemaManagerSecret     = config.getString("schemaManagerSecret")
  val log: (String) => Unit   = (x: String) => logger.info(x)
  val errorHandlerFactory     = ErrorHandlerFactory(log)
  val requestPrefix           = inject[String](identified by "request-prefix")

  val innerRoutes = extractRequest { _ =>
    val requestLogger = new RequestLogger(requestPrefix + ":schema-manager", log = log)
    val requestId     = requestLogger.begin

    handleExceptions(errorHandlerFactory.akkaHttpHandler(requestId)) {
      pathPrefix(Segment) { projectId =>
        get {
          optionalHeaderValueByName("Authorization") {
            case Some(authorizationHeader) if authorizationHeader == s"Bearer $schemaManagerSecret" =>
              parameters('forceRefresh ? false) { forceRefresh =>
                complete(performRequest(projectId, forceRefresh, requestLogger))
              }

            case Some(h) =>
              println(s"Wrong Authorization Header supplied: '$h'")
              complete(Unauthorized -> "Wrong Authorization Header supplied")

            case None =>
              println("No Authorization Header supplied")
              complete(Unauthorized -> "No Authorization Header supplied")
          }
        }
      }
    }
  }

  def performRequest(projectId: String, forceRefresh: Boolean, requestLogger: RequestLogger) = {
    getSchema(projectId, forceRefresh)
      .map(res => OK -> res)
      .andThen {
        case _ => requestLogger.end(Some(projectId), None)
      }
      .recover {
        case error: Throwable =>
          val unhandledErrorLogger = errorHandlerFactory.unhandledErrorHandler(
            requestId = requestLogger.requestId,
            projectId = Some(projectId)
          )

          BadRequest -> unhandledErrorLogger(error)._2.toString
      }
  }

  def getSchema(projectId: String, forceRefresh: Boolean): Future[String] = {
    val project: Future[Option[ProjectWithClientId]] = forceRefresh match {
      case true =>
        for {
          projectWithClientId <- uncachedProjectResolver.resolveProjectWithClientId(projectId)
          _                   <- cachedProjectResolver.invalidate(projectId)
        } yield {
          projectWithClientId
        }

      case false =>
        cachedProjectResolver.resolveProjectWithClientId(projectId)
    }

    project map {
      case None         => throw InvalidProjectId(projectId)
      case Some(schema) => SchemaSerializer.serialize(schema)
    }
  }

  def healthCheck =
    for {
      internalDb <- internalDatabase.run(sql"SELECT 1".as[Int])
    } yield internalDb
}
