package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.mutactions.client.DeleteRelationTable
import cool.graph.system.mutactions.internal.{BumpProjectRevision, DeleteField, DeleteRelation, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class DeleteRelationMutation(
    client: Client,
    project: Project,
    args: DeleteRelationInput,
    projectDbsFn: models.Project => InternalAndProjectDbs,
    clientDbQueries: ClientDbQueries
)(implicit inj: Injector)
    extends InternalProjectMutation[DeleteRelationMutationPayload] {

  val relation: Relation          = project.getRelationById_!(args.relationId)
  val relationFields: List[Field] = project.getFieldsByRelationId(relation.id)

  val updatedModels: List[Model] = relationFields.map { field =>
    val model = project.getModelByFieldId_!(field.id)
    model.copy(fields = model.fields.filter(_.id != field.id))
  }

  val updatedProject: Project = project.copy(relations = project.relations.filter(_.id != relation.id),
                                             models = project.models.filter(model => !updatedModels.map(_.id).contains(model.id)) ++ updatedModels)

  override def prepareActions(): List[Mutaction] = {

    actions = relationFields.map { field =>
      DeleteField(project = project, model = project.getModelByFieldId_!(field.id), field = field, allowDeleteRelationField = true)
    } ++
      List(
        DeleteRelation(relation, project, clientDbQueries),
        DeleteRelationTable(project = project, relation = relation),
        BumpProjectRevision(project = project),
        InvalidateSchema(project = project)
      )

    actions
  }

  override def getReturnValue: Option[DeleteRelationMutationPayload] = {
    Some(DeleteRelationMutationPayload(clientMutationId = args.clientMutationId, project = updatedProject, relation = relation))
  }
}

case class DeleteRelationMutationPayload(clientMutationId: Option[String], project: models.Project, relation: models.Relation) extends Mutation

case class DeleteRelationInput(clientMutationId: Option[String], relationId: String)
