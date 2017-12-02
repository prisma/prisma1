package cool.graph.deploy.database.persistence

import cool.graph.shared.models.{Migration, Project, UnappliedMigration}

import scala.concurrent.Future

trait MigrationPersistence {
  def loadAll(projectId: String): Future[Seq[Migration]]

  def getUnappliedMigration(): Future[Option[UnappliedMigration]]

  def create(project: Project, migration: Migration): Future[Migration]
  def getNextMigration(projectId: String): Future[Option[Migration]]
  def getLastMigration(projectId: String): Future[Option[Migration]]

  def markMigrationAsApplied(migration: Migration): Future[Unit]
}
