package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.{ProjectTable, Tables}
import cool.graph.shared.models.{MigrationSteps, Project}
import slick.jdbc.MySQLProfile.backend.DatabaseDef
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

case class ProjectPersistenceImpl(
    internalDatabase: DatabaseDef
)(implicit ec: ExecutionContext)
    extends ProjectPersistence {

  override def load(id: String): Future[Option[Project]] = {
    internalDatabase
      .run(ProjectTable.currentProjectById(id))
      .map(_.map { projectRow =>
        DbToModelMapper.convert(projectRow)
      })
  }

  override def save(project: Project, migrationSteps: MigrationSteps): Future[Unit] = {
    for {
      currentProject      <- load(project.id)
      dbProject           = ModelToDbMapper.convert(project)
      withRevisionBunmped = dbProject.copy(revision = currentProject.map(_.revision).getOrElse(0) + 1)
      addProject          = Tables.Projects += withRevisionBunmped
      _                   <- internalDatabase.run(addProject).map(_ => ())
    } yield ()
  }
}
