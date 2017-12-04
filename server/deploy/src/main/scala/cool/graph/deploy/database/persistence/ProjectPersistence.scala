package cool.graph.deploy.database.persistence

import cool.graph.shared.models.Project

import scala.concurrent.Future

trait ProjectPersistence {
  def load(id: String): Future[Option[Project]]
  def loadAll(): Future[Seq[Project]]
  def create(project: Project): Future[Unit]
}
