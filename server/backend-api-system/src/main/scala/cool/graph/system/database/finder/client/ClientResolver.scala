package cool.graph.system.database.finder.client

import cool.graph.shared.models.{Client, Project}
import cool.graph.system.database.finder.{CachedProjectResolver, ProjectResolver}
import cool.graph.system.database.tables.Tables.{Clients, Seats}
import cool.graph.system.database.{DbToModelMapper, tables}
import cool.graph.system.metrics.SystemMetrics
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.{ExecutionContext, Future}

trait ClientResolver {
  def resolve(clientId: String): Future[Option[Client]]
  def resolveProjectsForClient(clientId: String): Future[Vector[Project]]
}

object ClientResolver {
  def apply(internalDatabase: DatabaseDef, projectResolver: ProjectResolver)(implicit ec: ExecutionContext): ClientResolver = {
    ClientResolverImpl(internalDatabase, projectResolver)
  }
}

case class ClientResolverImpl(
    internalDatabase: DatabaseDef,
    projectResolver: ProjectResolver
)(implicit ec: ExecutionContext)
    extends ClientResolver {
  import ClientResolverMetrics._

  override def resolve(clientId: String): Future[Option[Client]] = resolveClientTimer.timeFuture() {
    clientForId(clientId).map { clientRowOpt =>
      clientRowOpt.map { clientRow =>
        DbToModelMapper.createClient(clientRow)
      }
    }
  }

  private def clientForId(clientId: String): Future[Option[tables.Client]] = {
    val query = for {
      client <- Clients
      if client.id === clientId
    } yield client

    internalDatabase.run(query.result.headOption)
  }

  override def resolveProjectsForClient(clientId: String): Future[Vector[Project]] = resolveAllProjectsForClientTimer.timeFuture() {
    def resolveProjectIds(projectIds: Vector[String]): Future[Vector[Project]] = {
      val tmp: Vector[Future[Option[Project]]]       = projectIds.map(projectResolver.resolve)
      val sequenced: Future[Vector[Option[Project]]] = Future.sequence(tmp)
      sequenced.map(_.flatten)
    }

    for {
      projectIds <- projectIdsForClientId(clientId)
      projects   <- resolveProjectIds(projectIds)
    } yield projects
  }

  private def projectIdsForClientId(clientId: String): Future[Vector[String]] = readProjectIdsFromDatabaseTimer.timeFuture() {
    val query = for {
      seat <- Seats
      if seat.clientId === clientId
    } yield seat.projectId

    internalDatabase.run(query.result.map(_.toVector))
  }
}

object ClientResolverMetrics {
  import SystemMetrics._

  val resolveClientTimer               = defineTimer("readClientFromDatabaseTimer")
  val readProjectIdsFromDatabaseTimer  = defineTimer("readProjectIdsFromDatabaseTimer")
  val resolveAllProjectsForClientTimer = defineTimer("resolveAllProjectsForClientTimer")
}
