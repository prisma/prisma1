package cool.graph.system.mutations

import cool.graph._
import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.mutactions.client.{CreateRelationTable, DeleteRelationTable}
import cool.graph.system.mutactions.internal._
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global

case class UpdateRelationMutation(
    client: models.Client,
    project: models.Project,
    args: UpdateRelationInput,
    projectDbsFn: models.Project => InternalAndProjectDbs,
    clientDbQueries: ClientDbQueries
)(implicit inj: Injector)
    extends InternalProjectMutation[UpdateRelationMutationPayload]
    with Injectable {

  val relation: Relation = project.getRelationById_!(args.id)

  val leftModel: Model  = project.getModelById_!(relation.modelAId)
  val rightModel: Model = project.getModelById_!(relation.modelBId)

  val fieldOnLeftModel: Field  = relation.getModelAField_!(project)
  val fieldOnRightModel: Field = relation.getModelBField_!(project)

  val updatedFieldOnLeftModel: Option[models.Field] =
    updateField(args.fieldOnLeftModelName, args.fieldOnLeftModelIsList, args.fieldOnLeftModelIsRequired, args.leftModelId.isDefined, fieldOnLeftModel)

  var updatedFieldOnRightModel: Option[models.Field] =
    updateField(args.fieldOnRightModelName, args.fieldOnRightModelIsList, args.fieldOnRightModelIsRequired, args.rightModelId.isDefined, fieldOnRightModel)

  val updatedRelation: Option[models.Relation] = updateRelation(args.name, args.description, args.leftModelId, args.rightModelId)

  val updatedProject: (Model, Model, Relation, Project) = getUpdatedProject
  var migrationActions: List[Mutaction]                 = List()

  def isSameFieldOnSameModel: Boolean = {
    updatedFieldOnLeftModel
      .getOrElse(fieldOnLeftModel)
      .name == updatedFieldOnRightModel
      .getOrElse(fieldOnRightModel)
      .name && args.leftModelId.getOrElse(leftModel.id) == args.rightModelId
      .getOrElse(rightModel.id)
  }

  def wasSameFieldOnSameModel: Boolean = {
    fieldOnLeftModel.name == fieldOnRightModel.name &&
    leftModel.id == rightModel.id
  }

  override def prepareActions(): List[Mutaction] = {

    if (args.leftModelId.getOrElse(leftModel.id) == args.rightModelId
          .getOrElse(rightModel.id) && args.fieldOnLeftModelName.getOrElse(fieldOnLeftModel.name) == args.fieldOnRightModelName
          .getOrElse(fieldOnRightModel.name) && args.fieldOnLeftModelIsList.getOrElse(fieldOnLeftModel.isList) != args.fieldOnRightModelIsList
          .getOrElse(fieldOnRightModel.isList)) {
      actions = List(InvalidInput(UserInputErrors.OneToManyRelationSameModelSameField()))
      return actions
    }

    if (modifiesModels) {
      migrationActions :+= InvalidInput(UserInputErrors.EdgesAlreadyExist(), edgesExist)

      migrationActions :+= DeleteRelationTable(project = project, relation = relation)

      val newRelation = relation.copy(modelAId = args.leftModelId.getOrElse(leftModel.id), modelBId = args.rightModelId.getOrElse(rightModel.id))

      migrationActions :+= CreateRelationTable(project = project, relation = newRelation)
    }

    if (updatedFieldOnLeftModel.isDefined || updatedFieldOnRightModel.isDefined) {

      if (isSameFieldOnSameModel) {
        if (!wasSameFieldOnSameModel)
          migrationActions :+= DeleteField(
            project = project,
            model = rightModel,
            field = fieldOnRightModel,
            allowDeleteRelationField = true
          )

        migrationActions :+=
          UpdateField(
            model = leftModel,
            oldField = fieldOnLeftModel,
            field = updatedFieldOnLeftModel.getOrElse(fieldOnLeftModel),
            migrationValue = None,
            newModelId = args.leftModelId,
            clientDbQueries = clientDbQueries
          )
      } else {
        migrationActions :+=
          UpdateField(
            model = leftModel,
            oldField = fieldOnLeftModel,
            field = updatedFieldOnLeftModel.getOrElse(fieldOnLeftModel),
            migrationValue = None,
            newModelId = args.leftModelId,
            clientDbQueries = clientDbQueries
          )

        if (wasSameFieldOnSameModel) {
          updatedFieldOnRightModel = Some(
            models.Field(
              id = Cuid.createCuid(),
              name = args.fieldOnRightModelName.getOrElse(fieldOnRightModel.name),
              typeIdentifier = TypeIdentifier.Relation,
              isRequired = false,
              isList = args.fieldOnRightModelIsList.getOrElse(fieldOnRightModel.isList),
              isUnique = false,
              isSystem = false,
              isReadonly = false,
              relation = Some(relation),
              relationSide = Some(RelationSide.B)
            ))
          migrationActions :+= CreateField(project, rightModel, updatedFieldOnRightModel.get, None, clientDbQueries)
        } else {
          migrationActions :+=
            UpdateField(
              model = rightModel,
              oldField = fieldOnRightModel,
              field = updatedFieldOnRightModel.getOrElse(fieldOnRightModel),
              migrationValue = None,
              newModelId = args.rightModelId,
              clientDbQueries = clientDbQueries
            )
        }
      }
    }

    updatedRelation.foreach(relation => migrationActions :+= UpdateRelation(oldRelation = relation, relation = relation, project = project))

    actions = migrationActions :+ BumpProjectRevision(project = project) :+ InvalidateSchema(project = project)
    actions
  }

  override def getReturnValue: Option[UpdateRelationMutationPayload] = {
    val (updatedLeftModel: Model, updatedRightModel: Model, finalRelation: Relation, updatedProject: Project) = getUpdatedProject

    Some(
      UpdateRelationMutationPayload(
        clientMutationId = args.clientMutationId,
        project = updatedProject,
        leftModel = updatedLeftModel,
        rightModel = updatedRightModel,
        relation = finalRelation
      ))
  }

  def updateField(fieldNameArg: Option[String],
                  fieldListArg: Option[Boolean],
                  fieldRequiredArg: Option[Boolean],
                  modelChanged: Boolean,
                  existingField: models.Field): Option[models.Field] = {

    if (modelChanged || fieldNameArg.isDefined || fieldListArg.isDefined || fieldRequiredArg.isDefined) {
      Some(
        existingField.copy(
          name = fieldNameArg.getOrElse(existingField.name),
          isList = fieldListArg.getOrElse(existingField.isList),
          isRequired = fieldRequiredArg.getOrElse(existingField.isRequired)
        ))
    } else
      None
  }

  def updateRelation(nameArg: Option[String],
                     descriptionArg: Option[String],
                     leftModelIdArg: Option[String],
                     rightModelIdArg: Option[String]): Option[models.Relation] = {

    if (nameArg.isDefined || descriptionArg.isDefined || leftModelIdArg.isDefined || rightModelIdArg.isDefined) {
      Some(
        relation.copy(
          name = nameArg.getOrElse(relation.name),
          description = descriptionArg match {
            case Some(description) => Some(description)
            case None              => relation.description
          },
          modelAId = leftModelIdArg.getOrElse(relation.modelAId),
          modelBId = rightModelIdArg.getOrElse(relation.modelBId)
        ))
    } else None
  }

  def isDifferent(arg: Option[Any], existing: Any) = arg.getOrElse(existing) != existing

  def modifiesModels = isDifferent(args.rightModelId, rightModel.id) || isDifferent(args.leftModelId, leftModel.id)

  def edgesExist = clientDbQueries.itemCountForRelation(relation).map(_ != 0)

  def getUpdatedProject: (Model, Model, Relation, Project) = {
    val updatedLeftModel = leftModel.copy(
      fields =
        leftModel.fields
          .filter(_.id != fieldOnLeftModel.id) :+ updatedFieldOnLeftModel
          .getOrElse(fieldOnLeftModel))
    val updatedRightModel = rightModel.copy(
      fields =
        rightModel.fields
          .filter(_.id != fieldOnRightModel.id) :+ updatedFieldOnRightModel
          .getOrElse(fieldOnRightModel))
    val finalRelation = updatedRelation.getOrElse(relation)

    val updatedProject = project.copy(
      models = project.models.map {
        case x: Model if x.id == leftModel.id  => updatedLeftModel
        case x: Model if x.id == rightModel.id => updatedRightModel
        case x                                 => x
      },
      relations = project.relations.map {
        case x if x.id == finalRelation.id => finalRelation
        case x                             => x
      }
    )
    (updatedLeftModel, updatedRightModel, finalRelation, updatedProject)
  }
}

case class UpdateRelationMutationPayload(clientMutationId: Option[String],
                                         project: models.Project,
                                         leftModel: models.Model,
                                         rightModel: models.Model,
                                         relation: models.Relation)
    extends Mutation

case class UpdateRelationInput(clientMutationId: Option[String],
                               id: String,
                               description: Option[String],
                               name: Option[String],
                               leftModelId: Option[String],
                               rightModelId: Option[String],
                               fieldOnLeftModelName: Option[String],
                               fieldOnRightModelName: Option[String],
                               fieldOnLeftModelIsList: Option[Boolean],
                               fieldOnRightModelIsList: Option[Boolean],
                               fieldOnLeftModelIsRequired: Option[Boolean],
                               fieldOnRightModelIsRequired: Option[Boolean])
