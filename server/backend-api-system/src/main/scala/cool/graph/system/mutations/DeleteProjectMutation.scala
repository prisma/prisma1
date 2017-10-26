package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.{Client, Project}
import cool.graph.system.database.finder.ProjectQueries
import cool.graph.system.mutactions.client.DeleteClientDatabaseForProject
import cool.graph.system.mutactions.internal.{DeleteProject, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

case class DeleteProjectMutation(
    client: Client,
    project: Project,
    args: DeleteProjectInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[DeleteProjectMutationPayload]
    with Injectable {

  val projectQueries: ProjectQueries = inject[ProjectQueries](identified by "projectQueries")

  override def prepareActions(): List[Mutaction] = {

    actions :+= DeleteProject(client = client, project = project, projectQueries = projectQueries, internalDatabase = internalDatabase.databaseDef)
    actions :+= DeleteClientDatabaseForProject(project.id)
    actions :+= InvalidateSchema(project = project)

    actions
  }

  override def getReturnValue: Option[DeleteProjectMutationPayload] = {
    Some {
      DeleteProjectMutationPayload(
        clientMutationId = args.clientMutationId,
        client = client.copy(projects = client.projects.filter(_.id != project.id)),
        project = project
      )
    }
  }
}

case class DeleteProjectMutationPayload(
    clientMutationId: Option[String],
    client: models.Client,
    project: models.Project
) extends Mutation

case class DeleteProjectInput(clientMutationId: Option[String], projectId: String)
