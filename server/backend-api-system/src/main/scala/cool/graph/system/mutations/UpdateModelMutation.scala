package cool.graph.system.mutations

import cool.graph.Types.Id
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.{Client, Model, Project}
import cool.graph.system.mutactions.client.RenameTable
import cool.graph.system.mutactions.internal.{BumpProjectRevision, InvalidateSchema, UpdateModel}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class UpdateModelMutation(
    client: Client,
    project: Project,
    args: UpdateModelInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[UpdateModelMutationPayload] {

  val model: Model = project.getModelById_!(args.modelId)

  var updatedModel: models.Model     = mergeInputValuesToModel(model, args)
  val updatedProject: models.Project = project.copy(models = project.models.filter(_.id != model.id) :+ updatedModel)

  def mergeInputValuesToModel(existingModel: Model, updateValues: UpdateModelInput): Model = {
    existingModel.copy(
      description = updateValues.description.orElse(existingModel.description),
      name = updateValues.name.getOrElse(existingModel.name),
      fieldPositions = args.fieldPositions.getOrElse(existingModel.fieldPositions)
    )
  }

  override def prepareActions(): List[Mutaction] = {
    val updateModel = UpdateModel(project = project, oldModel = model, model = updatedModel)
    val updateTable = if (args.name.contains(model.name)) {
      None
    } else {
      args.name.map(RenameTable(project.id, model, _))
    }

    actions = updateTable match {
      case Some(updateTable) => List(updateModel, updateTable, InvalidateSchema(project), BumpProjectRevision(project))
      case None              => List(updateModel, InvalidateSchema(project), BumpProjectRevision(project))
    }
    actions
  }

  override def getReturnValue: Option[UpdateModelMutationPayload] = {
    Some(UpdateModelMutationPayload(clientMutationId = args.clientMutationId, project = updatedProject, model = updatedModel))
  }
}

case class UpdateModelMutationPayload(clientMutationId: Option[String], model: models.Model, project: models.Project) extends Mutation

case class UpdateModelInput(clientMutationId: Option[String],
                            modelId: String,
                            description: Option[String],
                            name: Option[String],
                            fieldPositions: Option[List[Id]])
    extends MutationInput
