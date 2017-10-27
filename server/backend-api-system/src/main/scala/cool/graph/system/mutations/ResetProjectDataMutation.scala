package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.system.mutactions.client.{DeleteAllDataItems, DeleteAllRelations}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class ResetProjectDataMutation(
    client: Client,
    project: Project,
    args: ResetProjectDataInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[ResetProjectDataMutationPayload] {

  override def prepareActions(): List[Mutaction] = {

    val removeRelations =
      project.relations.map(relation => DeleteAllRelations(projectId = project.id, relation = relation))

    actions ++= removeRelations

    val removeDataItems = project.models.map(model => DeleteAllDataItems(projectId = project.id, model = model))

    actions ++= removeDataItems

    actions
  }

  override def getReturnValue: Option[ResetProjectDataMutationPayload] = {
    Some(ResetProjectDataMutationPayload(clientMutationId = args.clientMutationId, client = client, project = project))
  }
}

case class ResetProjectDataMutationPayload(clientMutationId: Option[String], client: models.Client, project: models.Project) extends Mutation

case class ResetProjectDataInput(clientMutationId: Option[String], projectId: String)
