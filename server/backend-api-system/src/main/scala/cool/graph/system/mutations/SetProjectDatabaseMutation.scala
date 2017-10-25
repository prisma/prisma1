package cool.graph.system.mutations

import com.typesafe.config.Config
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.SystemErrors.InvalidProjectDatabase
import cool.graph.shared.models
import cool.graph.shared.models.{Client, Project, ProjectDatabase}
import cool.graph.system.database.finder.{ProjectDatabaseFinder, ProjectQueries}
import cool.graph.system.mutactions.internal.{InvalidateSchema, UpdateProject}
import cool.graph.{InternalProjectMutation, Mutaction}

import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

import scala.concurrent.Await

case class SetProjectDatabaseMutation(
    args: SetProjectDatabaseInput,
    project: Project,
    client: Client,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[SetProjectDatabaseMutationPayload]
    with Injectable {
  import scala.concurrent.duration._

  val config: Config                 = inject[Config](identified by "config")
  val projectQueries: ProjectQueries = inject[ProjectQueries](identified by "projectQueries")

  val newProjectDatabase: ProjectDatabase =
    Await.result(ProjectDatabaseFinder.forId(args.projectDatabaseId)(internalDatabase.databaseDef), 5.seconds) match {
      case Some(x) => x
      case None    => throw InvalidProjectDatabase(args.projectDatabaseId)
    }

  val updatedProject: Project = project.copy(projectDatabase = newProjectDatabase)

  override def prepareActions(): List[Mutaction] = {
    val updateProject = UpdateProject(
      client = client,
      oldProject = project,
      project = updatedProject,
      internalDatabase = internalDatabase.databaseDef,
      projectQueries = projectQueries,
      bumpRevision = false
    )
    val invalidateSchema = InvalidateSchema(project = project)
    actions = List(updateProject, invalidateSchema)

    actions
  }

  override def getReturnValue: Option[SetProjectDatabaseMutationPayload] = {
    Some(
      SetProjectDatabaseMutationPayload(
        clientMutationId = args.clientMutationId,
        client = client,
        project = updatedProject
      ))
  }
}

case class SetProjectDatabaseMutationPayload(clientMutationId: Option[String], client: models.Client, project: models.Project) extends Mutation

case class SetProjectDatabaseInput(clientMutationId: Option[String], projectId: String, projectDatabaseId: String)
