package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.{Client, Field, Model, Project}
import cool.graph.system.database.SystemFields
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.mutactions.client.{DeleteColumn, DeleteRelationTable}
import cool.graph.system.mutactions.internal.{BumpProjectRevision, DeleteField, DeleteRelation, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class DeleteFieldMutation(
    client: Client,
    project: Project,
    args: DeleteFieldInput,
    projectDbsFn: models.Project => InternalAndProjectDbs,
    clientDbQueries: ClientDbQueries
)(implicit inj: Injector)
    extends InternalProjectMutation[DeleteFieldMutationPayload] {

  val field: Field        = project.getFieldById_!(args.fieldId)
  val model: Model        = project.getModelByFieldId_!(args.fieldId)
  val updatedModel: Model = model.copy(fields = model.fields.filter(_.id != field.id))
  val updatedProject: Project = project.copy(models = project.models.map {
    case model if model.id == updatedModel.id => updatedModel
    case model                                => model
  })

  override def prepareActions(): List[Mutaction] = {
    if (field.isScalar) {
      if (SystemFields.isDeletableSystemField(field.name)) {
        // Only delete field in the project DB ("hiding" fields in the schema)
        actions :+= DeleteField(project, model = model, field = field, allowDeleteSystemField = true)
      } else {
        // Delete field in both DBs
        actions :+= DeleteField(project, model = model, field = field)
        actions :+= DeleteColumn(projectId = project.id, model = model, field = field)
      }
    } else {
      actions :+= DeleteField(project, model = model, field = field)
    }

    if (field.relation.isDefined) {
      val existingRelationFields = project.getFieldsByRelationId(field.relation.get.id)

      if (existingRelationFields.length == 1) {
        actions :+= DeleteRelation(relation = field.relation.get, project = project, clientDbQueries = clientDbQueries)
        actions :+= DeleteRelationTable(project = project, relation = field.relation.get)
      }
    }

    actions :+= BumpProjectRevision(project = project)
    actions :+= InvalidateSchema(project = project)
    actions
  }

  override def getReturnValue: Option[DeleteFieldMutationPayload] = {
    Some(DeleteFieldMutationPayload(clientMutationId = args.clientMutationId, model = updatedModel, field = field, project = updatedProject))
  }
}

case class DeleteFieldMutationPayload(clientMutationId: Option[String], model: models.Model, field: models.Field, project: Project) extends Mutation

case class DeleteFieldInput(clientMutationId: Option[String], fieldId: String)
