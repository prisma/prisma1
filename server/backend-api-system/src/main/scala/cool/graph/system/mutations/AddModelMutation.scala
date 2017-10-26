package cool.graph.system.mutations

import cool.graph.Types.Id
import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.Project
import cool.graph.system.database.SystemFields
import cool.graph.system.mutactions.client.CreateModelTable
import cool.graph.system.mutactions.internal.{BumpProjectRevision, CreateModel, CreateModelPermission, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class AddModelMutation(
    client: models.Client,
    project: models.Project,
    args: AddModelInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[AddModelMutationPayload] {

  val newModel: models.Model = models.Model(
    id = args.id,
    name = args.modelName,
    description = args.description,
    isSystem = false,
    fields = List(SystemFields.generateIdField()),
    fieldPositions = args.fieldPositions.getOrElse(List.empty)
  )

  val updatedProject: Project = project.copy(models = project.models :+ newModel)

  override def prepareActions(): List[Mutaction] = {
    // The client DB table will still have all system fields, even if they're not visible in the schema at first
    val clientTableModel  = newModel.copy(fields = newModel.fields :+ SystemFields.generateCreatedAtField() :+ SystemFields.generateUpdatedAtField())
    val createClientTable = CreateModelTable(projectId = project.id, model = clientTableModel)
    val addModelToProject = CreateModel(project = project, model = newModel)

    val createPublicPermissions: Seq[CreateModelPermission] = project.isEjected match {
      case true  => Seq.empty
      case false => models.ModelPermission.publicPermissions.map(CreateModelPermission(project, newModel, _))
    }

    actions = List(createClientTable, addModelToProject) ++ createPublicPermissions ++ List(BumpProjectRevision(project = project),
                                                                                            InvalidateSchema(project = project))
    actions
  }

  override def getReturnValue(): Option[AddModelMutationPayload] = {
    Some(AddModelMutationPayload(clientMutationId = args.clientMutationId, project = updatedProject, model = newModel))
  }
}

case class AddModelMutationPayload(clientMutationId: Option[String], project: models.Project, model: models.Model) extends Mutation

case class AddModelInput(
    clientMutationId: Option[String],
    projectId: String,
    modelName: String,
    description: Option[String],
    fieldPositions: Option[List[Id]]
) {
  val id: Id = Cuid.createCuid
}
