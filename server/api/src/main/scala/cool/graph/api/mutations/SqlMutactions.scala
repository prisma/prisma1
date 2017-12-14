package cool.graph.api.mutations

import cool.graph.api.database.mutactions.ClientSqlMutaction
import cool.graph.api.database.mutactions.mutactions._
import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.api.mutations.MutationTypes.ArgumentValue
import cool.graph.api.schema.APIErrors
import cool.graph.api.schema.APIErrors.RelationIsRequired
import cool.graph.cuid.Cuid.createCuid
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.{Field, Model, Project}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SqlMutactions(dataResolver: DataResolver) {
  case class ParentInfo(model: Model, field: Field, id: Id)
  case class CreateMutactionsResult(createMutaction: CreateDataItem, nestedMutactions: Seq[ClientSqlMutaction]) {
    def allMutactions: Vector[ClientSqlMutaction] = Vector(createMutaction) ++ nestedMutactions
  }

  def getMutactionsForDelete(model: Model, project: Project, id: Id, previousValues: DataItem): List[ClientSqlMutaction] = {

    val requiredRelationViolations     = model.relationFields.flatMap(field => checkIfRemovalWouldFailARequiredRelation(field, id, project))
    val removeFromConnectionMutactions = model.relationFields.map(field => RemoveDataItemFromManyRelationByToId(project.id, field, id))
    val deleteItemMutaction            = DeleteDataItem(project, model, id, previousValues)

    requiredRelationViolations ++ removeFromConnectionMutactions ++ List(deleteItemMutaction)
  }

  def getMutactionsForUpdate(project: Project, model: Model, args: CoolArgs, id: Id, previousValues: DataItem): List[ClientSqlMutaction] = {
    val updateMutaction = getUpdateMutaction(project, model, args, id, previousValues)
    val nested          = getMutactionsForNestedMutation(project, model, args, fromId = id)
    updateMutaction.toList ++ nested
  }

  def getMutactionsForCreate(
      project: Project,
      model: Model,
      args: CoolArgs,
      id: Id = createCuid(),
      parentInfo: Option[ParentInfo] = None
  ): CreateMutactionsResult = {

    val createMutaction = getCreateMutaction(project, model, args, id)
    val relationToParent = parentInfo.map { parent =>
      AddDataItemToManyRelation(project = project, fromModel = parent.model, fromField = parent.field, fromId = parent.id, toId = id, toIdAlreadyInDB = false)
    }

    val nested = getMutactionsForNestedMutation(project, model, args, fromId = id)

    CreateMutactionsResult(createMutaction = createMutaction, nestedMutactions = relationToParent.toVector ++ nested)
  }

  def getCreateMutaction(project: Project, model: Model, args: CoolArgs, id: Id): CreateDataItem = {
    val scalarArguments = for {
      field      <- model.scalarFields
      fieldValue <- args.getFieldValueAs[Any](field)
    } yield {
      if (field.isRequired && field.defaultValue.isDefined && fieldValue.isEmpty) {
        throw APIErrors.InputInvalid("null", field.name, model.name)
      }
      ArgumentValue(field.name, fieldValue)
    }

    CreateDataItem(
      project = project,
      model = model,
      values = scalarArguments :+ ArgumentValue("id", id),
      originalArgs = Some(args)
    )
  }

  def getUpdateMutaction(project: Project, model: Model, args: CoolArgs, id: Id, previousValues: DataItem): Option[UpdateDataItem] = {
    val scalarArguments = for {
      field      <- model.scalarFields.filter(_.name != "id")
      fieldValue <- args.getFieldValueAs[Any](field)
    } yield {
      ArgumentValue(field.name, fieldValue)
    }
    if (scalarArguments.nonEmpty) {
      Some(
        UpdateDataItem(
          project = project,
          model = model,
          id = id,
          values = scalarArguments,
          originalArgs = Some(args),
          previousValues = previousValues,
          itemExists = true
        ))
    } else None
  }

  def getMutactionsForNestedMutation(project: Project, model: Model, args: CoolArgs, fromId: Id): Seq[ClientSqlMutaction] = {
    val x = for {
      field          <- model.relationFields
      subModel       = field.relatedModel_!(project)
      nestedMutation <- args.subNestedMutation(field, subModel) // this is the input object containing the nested mutation
    } yield {
      getMutactionsForNestedCreateMutation(project, subModel, nestedMutation, ParentInfo(model, field, fromId)) ++
        getMutactionsForNestedConnectMutation(project, nestedMutation, ParentInfo(model, field, fromId))

    }
    x.flatten
  }

  def getMutactionsForNestedCreateMutation(
      project: Project,
      model: Model,
      nestedMutation: NestedMutation,
      parentInfo: ParentInfo
  ): Seq[ClientSqlMutaction] = {
    nestedMutation.creates.flatMap { create =>
      getMutactionsForCreate(project, model, create.data, parentInfo = Some(parentInfo)).allMutactions
    }
  }

  def getMutactionsForNestedConnectMutation(
      project: Project,
      nestedMutation: NestedMutation,
      parentInfo: ParentInfo
  ): Seq[ClientSqlMutaction] = {
    nestedMutation.connects.map { connect =>
      AddDataItemToManyRelationByUniqueField(
        project = project,
        fromModel = parentInfo.model,
        fromField = parentInfo.field,
        fromId = parentInfo.id,
        where = connect.where
      )
    }
  }

  private def checkIfRemovalWouldFailARequiredRelation(field: Field, fromId: String, project: Project): Option[InvalidInputClientSqlMutaction] = {
    val isInvalid = () => dataResolver.resolveByRelation(fromField = field, fromModelId = fromId, args = None).map(_.items.nonEmpty)

    runRequiredRelationCheckWithInvalidFunction(field, project, isInvalid)
  }

  private def runRequiredRelationCheckWithInvalidFunction(field: Field,
                                                          project: Project,
                                                          isInvalid: () => Future[Boolean]): Option[InvalidInputClientSqlMutaction] = {
    val relatedField = field.relatedFieldEager(project)
    val relatedModel = field.relatedModel_!(project)
    if (relatedField.isRequired && !relatedField.isList) {
      Some(InvalidInputClientSqlMutaction(RelationIsRequired(fieldName = relatedField.name, typeName = relatedModel.name), isInvalid = isInvalid))
    } else None
  }
}

sealed trait NestedMutation {
  val creates: Vector[CreateOne]
  val updates: Vector[UpdateOne]
  val upserts: Vector[UpsertOne]
  val deletes: Vector[DeleteOne]
  val connects: Vector[ConnectOne]
  val disconnects: Vector[DisconnectOne]
}

case class NestedManyMutation(
    create: Vector[CreateOne],
    update: Vector[UpdateOne],
    upsert: Vector[UpsertOne],
    delete: Vector[DeleteOne],
    connect: Vector[ConnectOne],
    disconnect: Vector[DisconnectOne]
) extends NestedMutation {
  override val creates     = create
  override val updates     = update
  override val upserts     = upsert
  override val deletes     = delete
  override val connects    = connect
  override val disconnects = disconnect
}

case class NestedOneMutation(
    create: Option[CreateOne],
    update: Option[UpdateOne],
    upsert: Option[UpsertOne],
    delete: Option[DeleteOne],
    connect: Option[ConnectOne],
    disconnect: Option[DisconnectOne]
) extends NestedMutation {
  override val creates     = create.toVector
  override val updates     = update.toVector
  override val upserts     = upsert.toVector
  override val deletes     = delete.toVector
  override val connects    = connect.toVector
  override val disconnects = disconnect.toVector
}

case class CreateOne(data: CoolArgs)
case class UpdateOne(where: NodeSelector, data: CoolArgs)
case class UpsertOne(where: NodeSelector, create: CoolArgs, update: CoolArgs)
case class DeleteOne(where: NodeSelector)
case class ConnectOne(where: NodeSelector)
case class DisconnectOne(where: NodeSelector)
