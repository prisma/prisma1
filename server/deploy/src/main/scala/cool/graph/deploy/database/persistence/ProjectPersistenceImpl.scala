package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.ProjectTable
import cool.graph.shared.models.{MigrationSteps, Project}
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.{ExecutionContext, Future}

case class ProjectPersistenceImpl(
    internalDatabase: DatabaseDef
)(implicit ec: ExecutionContext)
    extends ProjectPersistence {

  override def load(id: String): Future[Option[Project]] = {
    internalDatabase.run(ProjectTable.currentProjectById(id)).map(_.map(DbToModelMapper.convert))
  }

  override def save(project: Project, migrationSteps: MigrationSteps): Future[Unit] = {
    ???
  }
}
