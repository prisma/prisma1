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

import cool.graph.utils.boolean.BooleanUtils._

case class CreateMutactionsResult(createMutaction: CreateDataItem,
                                  scalarListMutactions: Vector[ClientSqlMutaction],
                                  nestedMutactions: Seq[ClientSqlMutaction]) {
  def allMutactions: Vector[ClientSqlMutaction] = Vector(createMutaction) ++ scalarListMutactions ++ nestedMutactions
}

case class ParentInfo(field: Field, where: NodeSelector) {
  val model = where.model
}

case class SqlMutactions(dataResolver: DataResolver) {
  val project = dataResolver.project

  def getMutactionsForDelete(model: Model, id: Id, previousValues: DataItem, outerWhere: NodeSelector): List[ClientSqlMutaction] = {
    val requiredRelationViolations     = model.relationFields.flatMap(field => checkIfRemovalWouldFailARequiredRelation(field, id, project))
    val removeFromConnectionMutactions = model.relationFields.map(field => RemoveDataItemFromManyRelationByToId(project.id, field, id))
    val deleteItemMutaction            = DeleteDataItem(project, model, id, previousValues)

    requiredRelationViolations ++ removeFromConnectionMutactions ++ List(deleteItemMutaction)
  }

  def getMutactionsForUpdate(model: Model, args: CoolArgs, id: Id, previousValues: DataItem, outerWhere: NodeSelector): List[ClientSqlMutaction] = {
    val updateMutaction = getUpdateMutaction(model, args, id, previousValues)
    val nested          = getMutactionsForNestedMutation(model, args, outerWhere)
    val scalarLists     = getMutactionsForScalarLists(model, args, nodeId = id)
    updateMutaction.toList ++ nested ++ scalarLists
  }

  def getMutactionsForCreate(model: Model, args: CoolArgs, id: Id = createCuid()): CreateMutactionsResult = {
    val createMutaction = getCreateMutaction(model, args, id)
    val nested          = getMutactionsForNestedMutation(model, args, NodeSelector.forId(model, id))
    val scalarLists     = getMutactionsForScalarLists(model, args, nodeId = id)

    CreateMutactionsResult(createMutaction = createMutaction, scalarListMutactions = scalarLists, nestedMutactions = nested)
  }

  def getSetScalarList(model: Model, field: Field, values: Vector[Any], id: Id): SetScalarList = {
    SetScalarList(
      project = project,
      model = model,
      field = field,
      values = values,
      nodeId = id
    )
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
    val scalarArguments = args.nonListScalarArguments(model)
    scalarArguments.nonEmpty.toOption {
      UpdateDataItem(
        project = project,
        model = model,
        id = id,
        values = scalarArguments,
        originalArgs = Some(args),
        previousValues = previousValues,
        itemExists = true
      )
    }
  }

  def getMutactionsForScalarLists(model: Model, args: CoolArgs, nodeId: Id): Vector[SetScalarList] = {
    val x = for {
      field  <- model.scalarListFields
      values <- args.subScalarList(field)
    } yield {
      values.values.nonEmpty.toOption {
        getSetScalarList(model, field, values.values, nodeId)
      }
    }
    x.flatten.toVector
  }

  def getMutactionsForNestedMutation(model: Model, args: CoolArgs, outerWhere: NodeSelector): Seq[ClientSqlMutaction] = {
    val x = for {
      field          <- model.relationFields
      subModel       = field.relatedModel_!(project)
      nestedMutation <- args.subNestedMutation(field, subModel) // this is the input object containing the nested mutation
    } yield {
      val parentInfo = ParentInfo(field, outerWhere)
      getMutactionsForWhereChecks(subModel, nestedMutation) ++
        getMutactionsForConnectionChecks(subModel, nestedMutation, parentInfo) ++
        getMutactionsForNestedCreateMutation(subModel, nestedMutation, parentInfo) ++
        getMutactionsForNestedConnectMutation(nestedMutation, parentInfo) ++
        getMutactionsForNestedDisconnectMutation(nestedMutation, parentInfo) ++
        getMutactionsForNestedDeleteMutation(nestedMutation, parentInfo) ++
        getMutactionsForNestedUpdateMutation(nestedMutation, parentInfo) ++
        getMutactionsForNestedUpsertMutation(subModel, nestedMutation, parentInfo)
    }
    x.flatten
  }

  def getMutactionsForWhereChecks(subModel: Model, nestedMutation: NestedMutation): Seq[ClientSqlMutaction] = {
    nestedMutation.updates.map(update => VerifyWhere(project, update.where)) ++
      nestedMutation.deletes.map(delete => VerifyWhere(project, delete.where)) ++
      nestedMutation.connects.map(connect => VerifyWhere(project, connect.where)) ++
      nestedMutation.disconnects.map(disconnect => VerifyWhere(project, disconnect.where))
  }

  def getMutactionsForConnectionChecks(subModel: Model, nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    val relation = project.relations.find(r => r.connectsTheModels(parentInfo.model, subModel)).get

    nestedMutation.updates.map(update => VerifyConnection(project, relation, outerWhere = parentInfo.where, innerWhere = update.where)) ++
      nestedMutation.deletes.map(delete => VerifyConnection(project, relation, outerWhere = parentInfo.where, innerWhere = delete.where)) ++
      nestedMutation.disconnects.map(disconnect => VerifyConnection(project, relation, outerWhere = parentInfo.where, innerWhere = disconnect.where))
  }

  def getMutactionsForNestedCreateMutation(model: Model, nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.creates.flatMap { create =>
      val id         = createCuid()
      val createItem = getCreateMutaction(model, create.data, id)
      val connectItem = AddDataItemToManyRelation(
        project = project,
        fromModel = parentInfo.model,
        fromField = parentInfo.field,
        fromId = parentInfo.where.fieldValueAsString,
        toId = id,
        toIdAlreadyInDB = false
      )

      List(createItem, connectItem) ++ getMutactionsForNestedMutation(model, create.data, NodeSelector.forId(model, id))
    }
  }

  def getMutactionsForNestedConnectMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.connects.map { connect =>
      AddDataItemToManyRelationByUniqueField(
        project = project,
        fromModel = parentInfo.model,
        fromField = parentInfo.field,
        fromId = parentInfo.where.fieldValueAsString,
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
        fromId = parentInfo.where.fieldValueAsString,
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
        fromId = parentInfo.where.fieldValueAsString,
        where = delete.where
      )
    }
  }

  def getMutactionsForNestedUpdateMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.updates.flatMap { update =>
      val updateMutaction = UpdateDataItemByUniqueFieldIfInRelationWith(
        project = project,
        fromModel = parentInfo.model,
        fromField = parentInfo.field,
        fromId = parentInfo.where.fieldValueAsString,
        where = update.where,
        args = update.data
      )
      List(updateMutaction) ++ getMutactionsForNestedMutation(update.where.model, update.data, update.where)
    }
  }

  def getMutactionsForNestedUpsertMutation(model: Model, nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.upserts.flatMap { upsert =>
      val upsertItem = UpsertDataItemIfInRelationWith(
        project = project,
        fromField = parentInfo.field,
        fromId = parentInfo.where.fieldValueAsString,
        createArgs = upsert.create,
        updateArgs = upsert.update,
        where = upsert.where
      )
      val addToRelation = AddDataItemToManyRelationByUniqueField(
        project = project,
        fromModel = parentInfo.model,
        fromField = parentInfo.field,
        fromId = parentInfo.where.fieldValueAsString,
        where = NodeSelector(model, model.getFieldByName_!("id"), GraphQLIdGCValue(upsertItem.idOfNewItem))
      )
      Vector(upsertItem, addToRelation)
    }
  }

  private def checkIfRemovalWouldFailARequiredRelation(field: Field, fromId: String, project: Project): Option[InvalidInputClientSqlMutaction] = {
    val isInvalid = () => dataResolver.resolveByRelation(fromField = field, fromModelId = fromId, args = None).map(_.items.nonEmpty)

    runRequiredRelationCheckWithInvalidFunction(field, isInvalid)
  }

  private def runRequiredRelationCheckWithInvalidFunction(field: Field, isInvalid: () => Future[Boolean]): Option[InvalidInputClientSqlMutaction] = {
    field.relatedField(project).flatMap { relatedField =>
      val relatedModel = field.relatedModel_!(project)

      (relatedField.isRequired && !relatedField.isList).toOption {
        InvalidInputClientSqlMutaction(RelationIsRequired(fieldName = relatedField.name, typeName = relatedModel.name), isInvalid = isInvalid)
      }
    }
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

case class ScalarListSet(values: Vector[Any])
