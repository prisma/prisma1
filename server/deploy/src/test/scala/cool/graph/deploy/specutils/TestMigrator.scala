package cool.graph.deploy.specutils

import akka.actor.ActorSystem
import cool.graph.deploy.database.persistence.{DbToModelMapper, MigrationPersistence}
import cool.graph.deploy.database.tables.{MigrationTable, ProjectTable}
import cool.graph.deploy.migration.{MigrationApplierImpl, Migrator}
import cool.graph.shared.models.{Migration, UnappliedMigration}
import cool.graph.utils.await.AwaitUtils
import cool.graph.utils.future.FutureUtils.FutureOpt
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.Future

case class TestMigrator(
    clientDatabase: DatabaseDef,
    internalDb: DatabaseDef,
    migrationPersistence: MigrationPersistence
)(implicit val system: ActorSystem)
    extends Migrator
    with AwaitUtils {
  import system.dispatcher
  val applier = MigrationApplierImpl(clientDatabase)

  // Execute the migration synchronously
  override def schedule(migration: Migration): Unit = {
    val unappliedMigration = (for {
      // it's easier to reload the migration from db instead of converting, for now.
      dbMigration                  <- FutureOpt(internalDb.run(MigrationTable.forRevision(migration.projectId, migration.revision)))
      previousProjectWithMigration <- FutureOpt(internalDb.run(ProjectTable.byIdWithMigration(migration.projectId)))
      previousProject              = DbToModelMapper.convert(previousProjectWithMigration._1, previousProjectWithMigration._2)
      nextProject                  = DbToModelMapper.convert(previousProjectWithMigration._1, dbMigration)
    } yield {
      UnappliedMigration(previousProject, nextProject, migration)
    }).future.await.get

    val migrated = for {
      result <- applier.applyMigration(unappliedMigration.previousProject, unappliedMigration.nextProject, migration)
    } yield {
      if (result.succeeded) {
        migrationPersistence.markMigrationAsApplied(migration)
      } else {
        Future.successful(())
      }
    }

    migrated.await
  }
}
