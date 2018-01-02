package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.{MigrationTable, ProjectTable, Tables}
import cool.graph.shared.models.{Migration, Project, UnappliedMigration}
import cool.graph.utils.future.FutureUtils.FutureOpt
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

  override def create(project: Project, migration: Migration): Future[Migration] = {
    for {
      latestMigration    <- internalDatabase.run(MigrationTable.lastMigrationForProject(migration.projectId))
      dbMigration        = ModelToDbMapper.convert(project, migration)
      withRevisionBumped = dbMigration.copy(revision = latestMigration.map(_.revision).getOrElse(0) + 1)
      addMigration       = Tables.Migrations += withRevisionBumped
      _                  <- internalDatabase.run(addMigration)
    } yield migration.copy(revision = withRevisionBumped.revision)
  }

  override def getUnappliedMigration(projectId: String): Future[Option[UnappliedMigration]] = {
    val x = for {
      unappliedMigration           <- FutureOpt(internalDatabase.run(MigrationTable.getUnappliedMigration(projectId)))
      previousProjectWithMigration <- FutureOpt(internalDatabase.run(ProjectTable.byIdWithMigration(projectId)))
    } yield {
      val previousProject = DbToModelMapper.convert(previousProjectWithMigration._1, previousProjectWithMigration._2)
      val nextProject     = DbToModelMapper.convert(previousProjectWithMigration._1, unappliedMigration)
      val _migration      = DbToModelMapper.convert(unappliedMigration)

      UnappliedMigration(previousProject, nextProject, _migration)
    }

    x.future
  }

  override def markMigrationAsApplied(migration: Migration): Future[Unit] = {
    internalDatabase.run(MigrationTable.markAsApplied(migration.projectId, migration.revision)).map(_ => ())
  }

  override def getLastMigration(projectId: String): Future[Option[Migration]] = {
    FutureOpt(internalDatabase.run(MigrationTable.lastAppliedMigrationForProject(projectId))).map(DbToModelMapper.convert).future
  }

  override def getNextMigration(projectId: String): Future[Option[Migration]] = {
    FutureOpt(internalDatabase.run(MigrationTable.nextUnappliedMigrationForProject(projectId))).map(DbToModelMapper.convert).future
  }
}
