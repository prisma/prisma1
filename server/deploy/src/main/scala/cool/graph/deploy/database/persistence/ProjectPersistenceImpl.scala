package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.{ProjectTable, Tables}
import cool.graph.shared.models.{MigrationSteps, Project, UnappliedMigration}
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
      currentProject     <- load(project.id)
      dbProject          = ModelToDbMapper.convert(project, migrationSteps)
      withRevisionBumped = dbProject.copy(revision = currentProject.map(_.revision).getOrElse(0) + 1)
      addProject         = Tables.Projects += withRevisionBumped
      _                  <- internalDatabase.run(addProject).map(_ => ())
    } yield ()
  }

  override def getUnappliedMigration(): Future[Option[UnappliedMigration]] = {
    internalDatabase.run(ProjectTable.unappliedMigrations()).map { dbProjects =>
      dbProjects.headOption.map { dbProject =>
        val project        = DbToModelMapper.convert(dbProject)
        val migrationSteps = DbToModelMapper.convertSteps(dbProject)
        UnappliedMigration(project, migrationSteps)
      }
    }
  }

  override def markMigrationAsApplied(project: Project, migrationSteps: MigrationSteps): Future[Unit] = {
    internalDatabase.run(ProjectTable.markAsApplied(project.id, project.revision)).map(_ => ())
  }
}
