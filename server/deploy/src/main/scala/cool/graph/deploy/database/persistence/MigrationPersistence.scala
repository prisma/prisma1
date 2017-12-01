package cool.graph.deploy.database.persistence

import cool.graph.shared.models.{Migration, Project, UnappliedMigration}

import scala.concurrent.Future

trait MigrationPersistence {
  def create(project: Project, migration: Migration): Future[Migration]

  def getUnappliedMigration(): Future[Option[UnappliedMigration]]

  def markMigrationAsApplied(migration: Migration): Future[Unit]
}
