package cool.graph.database

import cool.graph.deploy.database.persistence.ProjectPersistence
import cool.graph.shared.models.{MigrationSteps, Project, UnappliedMigration}

import scala.collection.mutable
import scala.concurrent.Future

class InMemoryProjectPersistence extends ProjectPersistence {
  case class Identifier(projectId: String, revision: Int)

  // Needs a better solution to work with ID and alias
  private val store = mutable.Map.empty[String, mutable.Buffer[Project]]

  override def load(id: String): Future[Option[Project]] = Future.successful {
    loadSync(id)
  }

  override def loadByIdOrAlias(idOrAlias: String): Future[Option[Project]] = Future.successful {
    loadSyncByIdOrAlias(idOrAlias)
  }

  private def loadSync(id: String): Option[Project] = {
    for {
      projectsWithId             <- store.get(id)
      projectWithHighestRevision <- projectsWithId.lastOption
    } yield projectWithHighestRevision
  }

  private def loadSyncByIdOrAlias(idOrAlias: String): Option[Project] = {
    for {
      projectsWithIdOrAlias      <- store.get(idOrAlias)
      projectWithHighestRevision <- projectsWithIdOrAlias.lastOption
    } yield projectWithHighestRevision
  }

  override def save(project: Project, migrationSteps: MigrationSteps): Future[Unit] = Future.successful {
    val currentProject     = loadSync(project.id)
    val withRevisionBumped = project.copy(revision = currentProject.map(_.revision).getOrElse(0) + 1)
    val projects           = store.getOrElseUpdate(project.id, mutable.Buffer.empty)

    projects.append(withRevisionBumped)
  }

  override def getUnappliedMigration(): Future[Option[UnappliedMigration]] = ???

  override def markMigrationAsApplied(project: Project, migrationSteps: MigrationSteps): Future[Unit] = ???
}
