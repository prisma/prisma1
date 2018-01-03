package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.{ProjectTable, Tables}
import cool.graph.shared.models.Project
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.{ExecutionContext, Future}

case class ProjectPersistenceImpl(
    internalDatabase: DatabaseDef
)(implicit ec: ExecutionContext)
    extends ProjectPersistence {

  override def load(id: String): Future[Option[Project]] = {
    internalDatabase
      .run(ProjectTable.byIdWithMigration(id))
      .map(_.map { projectWithMigration =>
        DbToModelMapper.convert(projectWithMigration._1, projectWithMigration._2)
      })
  }

  def loadNext(id: )

  override def create(project: Project): Future[Unit] = {
    val addProject = Tables.Projects += ModelToDbMapper.convert(project)
    internalDatabase.run(addProject).map(_ => ())
  }

  override def loadAll(): Future[Seq[Project]] = {
    internalDatabase.run(Tables.Projects.result).map(_.map(p => DbToModelMapper.convert(p)))
  }

  override def loadProjectsWithUnappliedMigrations(): Future[Seq[Project]] = {
    internalDatabase.run(ProjectTable.allWithUnappliedMigrations).map(_.map(p => DbToModelMapper.convert(p)))
  }
}
