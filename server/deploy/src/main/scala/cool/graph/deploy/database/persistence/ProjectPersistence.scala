package cool.graph.deploy.database.persistence

import cool.graph.shared.models.{Migration, Project, UnappliedMigration}

import scala.concurrent.Future

trait ProjectPersistence {
  def load(id: String): Future[Option[Project]]

//  def loadByIdOrAlias(idOrAlias: String): Future[Option[Project]]

  def save(project: Project): Future[Unit]
  def save(project: Project, migration: Migration): Future[Migration]

  def getUnappliedMigration(): Future[Option[UnappliedMigration]]

  def markMigrationAsApplied(migration: Migration): Future[Unit]
}
