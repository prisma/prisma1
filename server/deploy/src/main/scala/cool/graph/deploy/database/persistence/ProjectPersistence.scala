package cool.graph.deploy.database.persistence

import cool.graph.shared.models.{Migration, Project, UnappliedMigration}

import scala.concurrent.Future

trait ProjectPersistence {
  def load(id: String): Future[Option[Project]]
  def loadByIdOrAlias(idOrAlias: String): Future[Option[Project]]

  def create(project: Project): Future[Unit]
}
