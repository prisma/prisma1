package cool.graph.deploy.database.persistence

import cool.graph.shared.models.{MigrationSteps, Project}

import scala.concurrent.Future

trait ProjectPersistence {
  def load(id: String): Future[Option[Project]]

  def save(project: Project, migrationSteps: MigrationSteps): Future[Unit]
}
