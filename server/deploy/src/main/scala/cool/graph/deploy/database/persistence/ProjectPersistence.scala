package cool.graph.deploy.database.persistence

import cool.graph.shared.models.{MigrationSteps, Project, UnappliedMigration}

import scala.concurrent.Future

trait ProjectPersistence {
  def load(id: String): Future[Option[Project]]

  def loadByIdOrAlias(idOrAlias: String): Future[Option[Project]]

  def save(project: Project, migrationSteps: MigrationSteps): Future[Unit]

  def getUnappliedMigration(): Future[Option[UnappliedMigration]]

  def markMigrationAsApplied(project: Project, migrationSteps: MigrationSteps): Future[Unit]
}
