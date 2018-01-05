package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.{MigrationTable, Tables}
import cool.graph.shared.models.{Migration, MigrationId}
import cool.graph.shared.models.MigrationStatus.MigrationStatus
import cool.graph.utils.future.FutureUtils.FutureOpt
import play.api.libs.json.Json
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.{ExecutionContext, Future}

case class MigrationPersistenceImpl(
    internalDatabase: DatabaseDef
)(implicit ec: ExecutionContext)
    extends MigrationPersistence {

  override def loadAll(projectId: String): Future[Seq[Migration]] = {
    val baseQuery = for {
      migration <- Tables.Migrations
      if migration.projectId === projectId
    } yield migration

    val query = baseQuery.sortBy(_.revision.desc)
    internalDatabase.run(query.result).map(_.map(DbToModelMapper.convert))
  }

  override def create(migration: Migration): Future[Migration] = {
    for {
      lastRevision       <- internalDatabase.run(MigrationTable.lastRevision(migration.projectId))
      dbMigration        = ModelToDbMapper.convert(migration)
      withRevisionBumped = dbMigration.copy(revision = lastRevision.getOrElse(0) + 1)
      addMigration       = Tables.Migrations += withRevisionBumped
      _                  <- internalDatabase.run(addMigration)
    } yield migration.copy(revision = withRevisionBumped.revision)
  }

  override def updateMigrationStatus(id: MigrationId, status: MigrationStatus): Future[Unit] = {
    internalDatabase.run(MigrationTable.updateMigrationStatus(id.projectId, id.revision, status)).map(_ => ())
  }

  override def updateMigrationErrors(id: MigrationId, errors: Vector[String]): Future[Unit] = {
    val errorsJson = Json.toJson(errors)
    internalDatabase.run(MigrationTable.updateMigrationErrors(id.projectId, id.revision, errorsJson)).map(_ => ())
  }

  override def updateMigrationApplied(id: MigrationId, applied: Int): Future[Unit] = {
    internalDatabase.run(MigrationTable.updateMigrationApplied(id.projectId, id.revision, applied)).map(_ => ())
  }

  override def updateMigrationRolledBack(id: MigrationId, rolledBack: Int): Future[Unit] = {
    internalDatabase.run(MigrationTable.updateMigrationRolledBack(id.projectId, id.revision, rolledBack)).map(_ => ())
  }

  override def getLastMigration(projectId: String): Future[Option[Migration]] = {
    FutureOpt(internalDatabase.run(MigrationTable.lastSuccessfulMigration(projectId))).map(DbToModelMapper.convert).future
  }

  override def getNextMigration(projectId: String): Future[Option[Migration]] = {
    FutureOpt(internalDatabase.run(MigrationTable.nextOpenMigration(projectId))).map(DbToModelMapper.convert).future
  }

  override def loadDistinctUnmigratedProjectIds(): Future[Seq[String]] = {
    internalDatabase.run(MigrationTable.distinctUnmigratedProjectIds())
  }
}
