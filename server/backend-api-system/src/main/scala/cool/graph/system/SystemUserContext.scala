package cool.graph.system

import java.util.concurrent.TimeUnit

import cool.graph.SystemRequestContextTrait
import cool.graph.client.database.ProjectDataresolver
import cool.graph.cloudwatch.Cloudwatch
import cool.graph.shared.errors.SystemErrors
import cool.graph.shared.errors.UserInputErrors.InvalidSession
import cool.graph.shared.models.ModelOperation.ModelOperation
import cool.graph.shared.models._
import cool.graph.shared.queryPermissions.PermissionSchemaResolver
import cool.graph.system.authorization.SystemAuth
import cool.graph.system.database.finder.client.ClientResolver
import cool.graph.system.database.finder.{CachedProjectResolver, LogsDataResolver, ProjectResolver}
import cool.graph.system.database.tables.Tables.RelayIds
import cool.graph.system.schema.types.{SearchProviderAlgoliaSchemaResolver, ViewerModel}
import scaldi.{Injectable, Injector}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class SystemUserContext(
    client: Option[Client],
    requestId: String,
    log: scala.Predef.Function[String, Unit]
)(implicit inj: Injector, val clientResolver: ClientResolver)
    extends Injectable
    with SystemRequestContextTrait {

  override val projectId: Option[String] = None
  override val clientId                  = client.map(_.id).getOrElse("")
  override val requestIp                 = "fake-ip"

  val cloudwatch       = inject[Cloudwatch]("cloudwatch")
  val internalDatabase = inject[DatabaseDef](identified by "internal-db")
  val logsDatabase     = inject[DatabaseDef](identified by "logs-db")
  val projectResolver  = inject[ProjectResolver](identified by "projectResolver")
  val injector         = inj

  val logsDataResolver = new LogsDataResolver()

  def dataResolver(project: Project) = new ProjectDataresolver(project = project, requestContext = Some(this))

  val auth = new SystemAuth()

  def getTypeName(globalId: String): Future[Option[String]] = {
    if (globalId == ViewerModel.globalId) {
      return Future.successful(Some("Viewer"))
    }

    internalDatabase.run(
      RelayIds
        .filter(_.id === globalId)
        .map(_.typeName)
        .take(1)
        .result
        .headOption)
  }

  def getActionSchema(project: Project, payload: ActionSchemaPayload): Future[String] = {
    new ActionSchemaResolver().resolve(project, payload)
  }

  def getModelPermissionSchema(project: Project, modelId: String, operation: ModelOperation): Future[String] = {
    new PermissionSchemaResolver().resolve(project)
  }

  def getRelationPermissionSchema(project: Project, relationId: String): Future[String] = {
    new PermissionSchemaResolver().resolve(project)
  }

  def getSearchProviderAlgoliaSchema(project: Project, modelId: String): Future[String] = {
    new SearchProviderAlgoliaSchemaResolver().resolve(project, modelId)
  }

  def getClient: Client = client match {
    case Some(client) => client
    case None         => throw new InvalidSession
  }

  def refresh(clientId: String): SystemUserContext = refresh(Some(clientId))

  def refresh(clientId: Option[String] = None): SystemUserContext = {
    implicit val internalDatabase: DatabaseDef = inject[DatabaseDef](identified by "internal-db")

    (clientId match {
      case Some(clientId) => Some(clientId)
      case None           => client.map(_.id)
    }) match {
      case Some(clientId) =>
        Await.result(SystemUserContext
                       .fetchClient(clientId, requestId, log = log),
                     Duration(5, TimeUnit.SECONDS))
      case None =>
        throw new Exception(
          "Don't call refresh when client is None. Currently the UserContext is used both when there is a client and when there isn't. We should refactor that")
    }
  }
}

object SystemUserContext {

  def fetchClient(clientId: String, requestId: String, log: scala.Predef.Function[String, Unit])(implicit inj: Injector,
                                                                                                 clientResolver: ClientResolver): Future[SystemUserContext] = {
    clientResolver.resolve(clientId = clientId) map {
      case Some(client) =>
        SystemUserContext(client = Some(client), requestId = requestId, log = log)
      case None =>
        throw SystemErrors.InvalidClientId(clientId)
    }
  }
}
