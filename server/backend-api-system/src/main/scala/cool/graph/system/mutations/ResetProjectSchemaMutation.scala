package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.system.database.finder.ProjectQueries
import cool.graph.system.mutactions.client.DeleteClientDatabaseForProject
import cool.graph.system.mutactions.internal.{BumpProjectRevision, DeleteProject, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

case class ResetProjectSchemaMutation(
    client: Client,
    project: Project,
    args: ResetProjectSchemaInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[ResetProjectSchemaMutationPayload]
    with Injectable {

  val projectQueries: ProjectQueries = inject[ProjectQueries](identified by "projectQueries")

  override def prepareActions(): List[Mutaction] = {

    // delete existing tables, data and internal schema

    // note: cascading deletes will delete models, relations etc. and these are not created by CreateProject
    actions :+= DeleteProject(
      client = client,
      project = project,
      willBeRecreated = true,
      internalDatabase = internalDatabase.databaseDef,
      projectQueries = projectQueries
    )

    actions :+= DeleteClientDatabaseForProject(project.id)

    val userFields = AuthenticateCustomerMutation.generateUserFields
    val userModel  = AuthenticateCustomerMutation.generateUserModel.copy(fields = userFields)

    val fileFields = AuthenticateCustomerMutation.generateFileFields
    val fileModel  = AuthenticateCustomerMutation.generateFileModel.copy(fields = fileFields)

    val resetProject = Project(
      id = project.id,
      ownerId = client.id,
      name = project.name,
      alias = project.alias,
      seats = project.seats.filter(_.isOwner == false), // owner added by createInternalStructureForNewProject
      models = List(userModel, fileModel),
      projectDatabase = project.projectDatabase
    )

    val resettedClient = client.copy(projects = client.projects.filter(_.id != project.id))

    actions ++= AuthenticateCustomerMutation.createInternalStructureForNewProject(
      client = resettedClient,
      project = resetProject,
      projectQueries = projectQueries,
      internalDatabase = internalDatabase.databaseDef,
      ignoreDuplicateNameVerificationError = true
    )

    actions ++= AuthenticateCustomerMutation.createClientDatabaseStructureForNewProject(resettedClient, resetProject, internalDatabase.databaseDef)
    actions ++= AuthenticateCustomerMutation.createIntegrationsForNewProject(resetProject)
    actions :+= BumpProjectRevision(project = project)
    actions :+= InvalidateSchema(project = project)
    actions
  }

  override def getReturnValue: Option[ResetProjectSchemaMutationPayload] = {
    Some(ResetProjectSchemaMutationPayload(clientMutationId = args.clientMutationId, client = client, project = project))
  }
}

case class ResetProjectSchemaMutationPayload(clientMutationId: Option[String], client: models.Client, project: models.Project) extends Mutation

case class ResetProjectSchemaInput(clientMutationId: Option[String], projectId: String)
