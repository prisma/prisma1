package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.{Client, Project}
import cool.graph.system.database.finder.ProjectQueries
import cool.graph.system.mutactions.internal.{BumpProjectRevision, InvalidateSchema, UpdateProject}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class UpdateProjectMutation(
    client: Client,
    project: Project,
    args: UpdateProjectInput,
    projectDbsFn: models.Project => InternalAndProjectDbs,
    projectQueries: ProjectQueries
)(implicit inj: Injector)
    extends InternalProjectMutation[UpdateProjectMutationPayload] {

  var updatedProject: models.Project = mergeInputValuesToProject(project, args)

  def mergeInputValuesToProject(existingProject: Project, updateValues: UpdateProjectInput): Project = {
    existingProject.copy(
      name = updateValues.name.getOrElse(existingProject.name),
      alias = updateValues.alias.orElse(existingProject.alias),
      webhookUrl = updateValues.webhookUrl.orElse(existingProject.webhookUrl),
      allowQueries = updateValues.allowQueries.getOrElse(existingProject.allowQueries),
      allowMutations = updateValues.allowMutations.getOrElse(existingProject.allowMutations)
    )
  }

  override def prepareActions(): List[Mutaction] = {
    val updateProject = UpdateProject(
      client = client,
      oldProject = project,
      project = updatedProject,
      internalDatabase = internalDatabase.databaseDef,
      projectQueries = projectQueries
    )

    actions = List(updateProject, BumpProjectRevision(project = project), InvalidateSchema(project = project))
    actions
  }

  override def getReturnValue: Option[UpdateProjectMutationPayload] = {
    Some(
      UpdateProjectMutationPayload(
        clientMutationId = args.clientMutationId,
        client = client.copy(projects = client.projects.filter(_.id != project.id) :+ updatedProject),
        project = updatedProject
      )
    )
  }
}

case class UpdateProjectMutationPayload(clientMutationId: Option[String], client: models.Client, project: models.Project) extends Mutation

case class UpdateProjectInput(clientMutationId: Option[String],
                              projectId: String,
                              name: Option[String],
                              alias: Option[String],
                              webhookUrl: Option[String],
                              allowQueries: Option[Boolean],
                              allowMutations: Option[Boolean])
