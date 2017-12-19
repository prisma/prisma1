package cool.graph.api.mutations

import cool.graph.api.database.mutactions.ClientSqlMutaction
import cool.graph.api.database.mutactions.mutactions._
import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.api.mutations.MutationTypes.ArgumentValue
import cool.graph.api.schema.APIErrors
import cool.graph.api.schema.APIErrors.RelationIsRequired
import cool.graph.cuid.Cuid.createCuid
import cool.graph.gc_values.GraphQLIdGCValue
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.{Field, Model, Project}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ParentInfo(model: Model, field: Field, id: Id)
case class CreateMutactionsResult(createMutaction: CreateDataItem, nestedMutactions: Seq[ClientSqlMutaction]) {
  def allMutactions: Vector[ClientSqlMutaction] = Vector(createMutaction) ++ nestedMutactions
}

case class SqlMutactions(dataResolver: DataResolver) {
  val project = dataResolver.project

  def getMutactionsForDelete(model: Model, id: Id, previousValues: DataItem): List[ClientSqlMutaction] = {
    val requiredRelationViolations     = model.relationFields.flatMap(field => checkIfRemovalWouldFailARequiredRelation(field, id, project))
    val removeFromConnectionMutactions = model.relationFields.map(field => RemoveDataItemFromManyRelationByToId(project.id, field, id))
    val deleteItemMutaction            = DeleteDataItem(project, model, id, previousValues)

    requiredRelationViolations ++ removeFromConnectionMutactions ++ List(deleteItemMutaction)
  }

  def getMutactionsForUpdate(model: Model, args: CoolArgs, id: Id, previousValues: DataItem): List[ClientSqlMutaction] = {
    val updateMutaction = getUpdateMutaction(model, args, id, previousValues)
    val nested          = getMutactionsForNestedMutation(model, args, fromId = id)
    updateMutaction.toList ++ nested
  }

  def getMutactionsForCreate(
      model: Model,
      args: CoolArgs,
      id: Id = createCuid(),
      parentInfo: Option[ParentInfo] = None
  ): CreateMutactionsResult = {

    val createMutaction = getCreateMutaction(model, args, id)
    val relationToParent = parentInfo.map { parent =>
      AddDataItemToManyRelation(project = project, fromModel = parent.model, fromField = parent.field, fromId = parent.id, toId = id, toIdAlreadyInDB = false)
    }

    val nested = getMutactionsForNestedMutation(model, args, fromId = id)

    CreateMutactionsResult(createMutaction = createMutaction, nestedMutactions = relationToParent.toVector ++ nested)
  }

  def getCreateMutaction(model: Model, args: CoolArgs, id: Id): CreateDataItem = {
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

  def getUpdateMutaction(model: Model, args: CoolArgs, id: Id, previousValues: DataItem): Option[UpdateDataItem] = {
    val scalarArguments = args.scalarArguments(model)
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

  def getMutactionsForNestedMutation(model: Model, args: CoolArgs, fromId: Id): Seq[ClientSqlMutaction] = {
    val x = for {
      field          <- model.relationFields
      subModel       = field.relatedModel_!(project)
      nestedMutation <- args.subNestedMutation(field, subModel) // this is the input object containing the nested mutation
    } yield {
      val parentInfo = ParentInfo(model, field, fromId)
      getMutactionsForNestedCreateMutation(subModel, nestedMutation, parentInfo) ++
        getMutactionsForNestedConnectMutation(nestedMutation, parentInfo) ++
        getMutactionsForNestedDisconnectMutation(nestedMutation, parentInfo) ++
        getMutactionsForNestedDeleteMutation(nestedMutation, parentInfo) ++
        getMutactionsForNestedUpdateMutation(nestedMutation, parentInfo) ++
        getMutactionsForNestedUpsertMutation(subModel, nestedMutation, parentInfo)

    }
    x.flatten
  }

  def getMutactionsForNestedCreateMutation(model: Model, nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.creates.flatMap { create =>
      getMutactionsForCreate(model, create.data, parentInfo = Some(parentInfo)).allMutactions
    }
  }

  def getMutactionsForNestedConnectMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
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

  def getMutactionsForNestedDisconnectMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.disconnects.map { disconnect =>
      RemoveDataItemFromManyRelationByUniqueField(
        project = project,
        fromModel = parentInfo.model,
        fromField = parentInfo.field,
        fromId = parentInfo.id,
        where = disconnect.where
      )
    }
  }

  def getMutactionsForNestedDeleteMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.deletes.map { delete =>
      DeleteDataItemByUniqueFieldIfInRelationWith(
        project = project,
        fromModel = parentInfo.model,
        fromField = parentInfo.field,
        fromId = parentInfo.id,
        where = delete.where
      )
    }
  }

  def getMutactionsForNestedUpdateMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.updates.map { update =>
      UpdateDataItemByUniqueFieldIfInRelationWith(
        project = project,
        fromModel = parentInfo.model,
        fromField = parentInfo.field,
        fromId = parentInfo.id,
        where = update.where,
        args = update.data
      )
    }
  }

  def getMutactionsForNestedUpsertMutation(model: Model, nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.upserts.flatMap { upsert =>
      val upsertItem = UpsertDataItem(
        project = project,
        model = model,
        createArgs = upsert.create,
        updateArgs = upsert.update,
        where = upsert.where
      )
      val addToRelation = AddDataItemToManyRelationByUniqueField(
        project = project,
        fromModel = parentInfo.model,
        fromField = parentInfo.field,
        fromId = parentInfo.id,
        where = NodeSelector(model, "id", GraphQLIdGCValue(upsertItem.idOfNewItem))
      )
      Vector(upsertItem, addToRelation)
    }
  }

  private def checkIfRemovalWouldFailARequiredRelation(field: Field, fromId: String, project: Project): Option[InvalidInputClientSqlMutaction] = {
    val isInvalid = () => dataResolver.resolveByRelation(fromField = field, fromModelId = fromId, args = None).map(_.items.nonEmpty)

    runRequiredRelationCheckWithInvalidFunction(field, isInvalid)
  }

  private def runRequiredRelationCheckWithInvalidFunction(field: Field, isInvalid: () => Future[Boolean]): Option[InvalidInputClientSqlMutaction] = {
    val relatedField = field.relatedFieldEager(project)
    val relatedModel = field.relatedModel_!(project)

    if (relatedField.isRequired && !relatedField.isList) {
      Some(InvalidInputClientSqlMutaction(RelationIsRequired(fieldName = relatedField.name, typeName = relatedModel.name), isInvalid = isInvalid))
    } else None
  }
}

case class NestedMutation(
    creates: Vector[CreateOne],
    updates: Vector[UpdateOne],
    upserts: Vector[UpsertOne],
    deletes: Vector[DeleteOne],
    connects: Vector[ConnectOne],
    disconnects: Vector[DisconnectOne]
)

case class CreateOne(data: CoolArgs)
case class UpdateOne(where: NodeSelector, data: CoolArgs)
case class UpsertOne(where: NodeSelector, create: CoolArgs, update: CoolArgs)
case class DeleteOne(where: NodeSelector)
case class ConnectOne(where: NodeSelector)
case class DisconnectOne(where: NodeSelector)
