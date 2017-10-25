package cool.graph.system.mutations

import cool.graph.Types.Id
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.mutactions.client.{DeleteModelTable, DeleteRelationTable}
import cool.graph.system.mutactions.internal._
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class DeleteModelMutation(
    client: Client,
    project: Project,
    args: DeleteModelInput,
    projectDbsFn: models.Project => InternalAndProjectDbs,
    clientDbQueries: ClientDbQueries
)(implicit inj: Injector)
    extends InternalProjectMutation[DeleteModelMutationPayload] {

  val model: Model = project.getModelById_!(args.modelId)

  val relations: List[Relation] = model.relations
  val updatedProject: Project = {
    val modelsWithoutThisOne                     = project.models.filter(_.id != model.id)
    val modelsWithRelationFieldsToThisOneRemoved = modelsWithoutThisOne.map(_.withoutFieldsForRelations(relations))
    project.copy(models = modelsWithRelationFieldsToThisOneRemoved, relations = project.relations.filter(r => !model.relations.map(_.id).contains(r.id)))
  }

  val relationFieldIds: List[Id] = for {
    relation <- relations
    field    <- relation.fields(project)
  } yield field.id

  override def prepareActions(): List[Mutaction] = {
    actions ++= project.actions.collect {
      case action @ Action(_, _, _, _, _, _, Some(trigger), _) if trigger.modelId == model.id =>
        DeleteAction(project, action)
    }
    actions ++= relations.map(relation => DeleteRelation(relation, project, clientDbQueries))
    actions ++= relations.map(relation => DeleteRelationTable(project = project, relation))
    actions :+= DeleteModel(project, model = model)
    actions :+= DeleteModelTable(projectId = project.id, model = model)
    actions :+= BumpProjectRevision(project = project)
    actions :+= InvalidateSchema(project = project)

    actions
  }

  override def getReturnValue: Option[DeleteModelMutationPayload] = {
    Some(DeleteModelMutationPayload(clientMutationId = args.clientMutationId, model = model, deletedFieldIds = relationFieldIds, project = updatedProject))
  }
}

case class DeleteModelMutationPayload(clientMutationId: Option[String], model: models.Model, deletedFieldIds: List[String], project: models.Project)
    extends Mutation

case class DeleteModelInput(clientMutationId: Option[String], modelId: String)
