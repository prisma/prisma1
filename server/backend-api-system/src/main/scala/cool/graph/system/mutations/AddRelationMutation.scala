package cool.graph.system.mutations

import cool.graph._
import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models
import cool.graph.shared.models.{Model, Project, RelationSide, TypeIdentifier}
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.mutactions.client.CreateRelationTable
import cool.graph.system.mutactions.internal._
import sangria.relay.Mutation
import scaldi.Injector

case class AddRelationMutation(
    client: models.Client,
    project: models.Project,
    args: AddRelationInput,
    projectDbsFn: models.Project => InternalAndProjectDbs,
    clientDbQueries: ClientDbQueries
)(implicit inj: Injector)
    extends InternalProjectMutation[AddRelationMutationPayload] {

  val leftModel: Model  = project.getModelById_!(args.leftModelId)
  val rightModel: Model = project.getModelById_!(args.rightModelId)

  val newRelation: models.Relation =
    models.Relation(id = Cuid.createCuid(), name = args.name, description = args.description, modelAId = leftModel.id, modelBId = rightModel.id)

  val fieldOnLeftModel: Option[models.Field] = Some(
    models.Field(
      id = Cuid.createCuid(),
      name = args.fieldOnLeftModelName,
      typeIdentifier = TypeIdentifier.Relation,
      isRequired = if (args.fieldOnLeftModelIsList) false else args.fieldOnLeftModelIsRequired,
      isList = args.fieldOnLeftModelIsList,
      isUnique = false,
      isSystem = false,
      isReadonly = false,
      relation = Some(newRelation),
      relationSide = Some(RelationSide.A)
    ))

  val fieldOnRightModel: Option[models.Field] = if (args.leftModelId != args.rightModelId || args.fieldOnLeftModelName != args.fieldOnRightModelName) {
    Some(
      models.Field(
        id = Cuid.createCuid(),
        name = args.fieldOnRightModelName,
        typeIdentifier = TypeIdentifier.Relation,
        isRequired = if (args.fieldOnRightModelIsList) false else args.fieldOnRightModelIsRequired,
        isList = args.fieldOnRightModelIsList,
        isUnique = false,
        isSystem = false,
        isReadonly = false,
        relation = Some(newRelation),
        relationSide = Some(RelationSide.B)
      ))
  } else None

  private def updatedLeftModel  = leftModel.copy(fields = leftModel.fields ++ fieldOnLeftModel)
  private def updatedRightModel = rightModel.copy(fields = rightModel.fields ++ fieldOnRightModel)
  private def updatedSameModel  = leftModel.copy(fields = leftModel.fields ++ fieldOnLeftModel ++ fieldOnRightModel)

  val updatedProject: Project =
    project.copy(
      models = project.models.map {
        case x: models.Model if x.id == leftModel.id && x.id == rightModel.id => updatedSameModel
        case x: models.Model if x.id == leftModel.id                          => updatedLeftModel
        case x: models.Model if x.id == rightModel.id                         => updatedRightModel
        case x                                                                => x
      },
      relations = project.relations :+ newRelation
    )

  override def prepareActions(): List[Mutaction] = {

    if (args.leftModelId == args.rightModelId &&
        args.fieldOnLeftModelName == args.fieldOnRightModelName &&
        args.fieldOnLeftModelIsList != args.fieldOnRightModelIsList) {
      actions = List(InvalidInput(UserInputErrors.OneToManyRelationSameModelSameField()))
      return actions
    }

    actions = {

      val createPublicPermissions: Vector[CreateRelationPermission] = project.isEjected match {
        case true  => Vector.empty
        case false => models.RelationPermission.publicPermissions.map(CreateRelationPermission(project, newRelation, _)).toVector
      }

      List(
        CreateRelation(updatedProject, newRelation, args.fieldOnLeftModelIsRequired, args.fieldOnRightModelIsRequired, clientDbQueries),
        CreateRelationTable(updatedProject, newRelation),
        CreateField(project, leftModel, fieldOnLeftModel.get, None, clientDbQueries)
      ) ++
        // note: fieldOnRightModel can be None for self relations
        fieldOnRightModel.map(field => List(CreateField(project, rightModel, field, None, clientDbQueries))).getOrElse(List()) ++
        createPublicPermissions ++
        List(BumpProjectRevision(project = project), InvalidateSchema(project = project))
    }
    actions
  }

  override def getReturnValue: Option[AddRelationMutationPayload] = {

    Some(
      AddRelationMutationPayload(
        clientMutationId = args.clientMutationId,
        project = updatedProject,
        leftModel = if (leftModel.id == rightModel.id) updatedSameModel else updatedLeftModel,
        rightModel = if (leftModel.id == rightModel.id) updatedSameModel else updatedRightModel,
        relation = newRelation
      ))
  }
}

case class AddRelationMutationPayload(clientMutationId: Option[String],
                                      project: models.Project,
                                      leftModel: models.Model,
                                      rightModel: models.Model,
                                      relation: models.Relation)
    extends Mutation

case class AddRelationInput(clientMutationId: Option[String],
                            projectId: String,
                            description: Option[String],
                            name: String,
                            leftModelId: String,
                            rightModelId: String,
                            fieldOnLeftModelName: String,
                            fieldOnRightModelName: String,
                            fieldOnLeftModelIsList: Boolean,
                            fieldOnRightModelIsList: Boolean,
                            fieldOnLeftModelIsRequired: Boolean = false,
                            fieldOnRightModelIsRequired: Boolean = false)
