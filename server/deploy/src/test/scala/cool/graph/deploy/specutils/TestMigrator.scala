package cool.graph.deploy.specutils

import cool.graph.deploy.database.persistence.MigrationPersistence
import cool.graph.deploy.migration.{MigrationApplierImpl, Migrator}
import cool.graph.shared.models.Migration
import cool.graph.utils.await.AwaitUtils
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class TestMigrator(clientDatabase: DatabaseDef, migrationPersistence: MigrationPersistence) extends Migrator with AwaitUtils {
  val applier = MigrationApplierImpl(clientDatabase)

  // Execute the migration synchronously
  override def schedule(migration: Migration): Unit = {
    (for {
      previousProject <-
      nextProject <-
      result <- applier.applyMigration(prevProject, nextProject, migration)
      _ <- if (result.succeeded) {
            migrationPersistence.markMigrationAsApplied(migration)
          } else {
            Future.successful(())
          }
    } yield ()).await
  }
}
