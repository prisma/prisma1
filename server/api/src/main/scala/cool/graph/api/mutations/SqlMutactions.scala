package cool.graph.api.mutations

import cool.graph.api.database.mutactions.ClientSqlMutaction
import cool.graph.api.database.mutactions.mutactions._
import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.api.mutations.MutationTypes.ArgumentValue
import cool.graph.api.schema.APIErrors.RelationIsRequired
import cool.graph.cuid.Cuid.createCuid
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.{Field, Model, Project, Relation}
import cool.graph.util.gc_value.GCAnyConverter
import cool.graph.utils.boolean.BooleanUtils._

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class CreateMutactionsResult(createMutaction: CreateDataItem,
                                  scalarListMutactions: Vector[ClientSqlMutaction],
                                  nestedMutactions: Seq[ClientSqlMutaction]) {
  def allMutactions: Vector[ClientSqlMutaction] = Vector(createMutaction) ++ scalarListMutactions ++ nestedMutactions
}

case class ParentInfo(field: Field, where: NodeSelector) {
  val model: Model       = where.model
  val relation: Relation = field.relation.get
  assert(
    model.fields.exists(_.id == field.id),
    s"${model.name} does not contain the field ${field.name}. If this assertion fires, this mutaction is used wrong by the programmer."
  )
}

case class SqlMutactions(dataResolver: DataResolver) {
  val project = dataResolver.project

  def getMutactionsForDelete(model: Model, id: Id, previousValues: DataItem, outerWhere: NodeSelector): List[ClientSqlMutaction] = {
    val requiredRelationViolations     = model.relationFields.flatMap(field => checkIfRemovalWouldFailARequiredRelation(field, id, project))
    val removeFromConnectionMutactions = model.relationFields.map(field => RemoveDataItemFromManyRelationByToId(project.id, field, id))
    val deleteItemMutaction            = DeleteDataItem(project, model, id, previousValues)

    requiredRelationViolations ++ removeFromConnectionMutactions ++ List(deleteItemMutaction)
  }

  def getMutactionsForUpdate(args: CoolArgs, id: Id, previousValues: DataItem, outerWhere: NodeSelector): List[ClientSqlMutaction] = {
    val updateMutaction = getUpdateMutaction(outerWhere.model, args, id, previousValues)
    val nested          = getMutactionsForNestedMutation(args, outerWhere, triggeredFromCreate = false)
    val scalarLists     = getMutactionsForScalarLists(outerWhere, args)

    updateMutaction.toList ++ nested ++ scalarLists
  }

  def getMutactionsForCreate(model: Model, args: CoolArgs, id: Id = createCuid()): CreateMutactionsResult = {
    val where           = NodeSelector.forId(model, id)
    val createMutaction = getCreateMutaction(model, args, id)
    val nested          = getMutactionsForNestedMutation(args, where, triggeredFromCreate = true)
    val scalarLists     = getMutactionsForScalarLists(where, args)

    CreateMutactionsResult(createMutaction = createMutaction, scalarListMutactions = scalarLists, nestedMutactions = nested)
  }

  // we need to rethink this thoroughly, we need to prevent both branches of executing their nested mutations && scalarlist mutations at the same time

  def getMutactionsForUpsert(allArgs: CoolArgs, createArgs: CoolArgs, updateArgs: CoolArgs, id: Id, outerWhere: NodeSelector): List[ClientSqlMutaction] = {
    val idWhere           = NodeSelector.forId(outerWhere.model, createArgs.raw("id").toString)
    val upsertMutaction   = UpsertDataItem(project, outerWhere, createArgs, updateArgs)
    val whereFieldValue   = updateArgs.raw.get(outerWhere.field.name) // todo WRONG! only when update
    val currentOuterWhere = if (whereFieldValue.isDefined) generateUpdatedWhere(outerWhere, whereFieldValue.get) else outerWhere
    val updateNested      = getMutactionsForNestedMutation(allArgs.updateArgumentsAsCoolArgs, currentOuterWhere, triggeredFromCreate = false)
    val createNested      = getMutactionsForNestedMutation(allArgs.createArgumentsAsCoolArgs, idWhere, triggeredFromCreate = true)
    val scalarListsCreate = getMutactionsForScalarLists(outerWhere, allArgs.createArgumentsAsCoolArgs)
    val scalarListsUpdate = getMutactionsForScalarLists(outerWhere, allArgs.updateArgumentsAsCoolArgs)

    List(upsertMutaction) ++ scalarListsUpdate ++ scalarListsCreate ++ updateNested ++ createNested
  }

  def generateUpdatedWhere(where: NodeSelector, updatedValue: Any): NodeSelector = {
    val unwrapped = updatedValue match {
      case Some(x) => x
      case x       => x
    }

    val newGCValue = GCAnyConverter(where.field.typeIdentifier, false).toGCValue(unwrapped).get
    where.copy(fieldValue = newGCValue)
  }

  def getSetScalarList(where: NodeSelector, field: Field, values: Vector[Any]): SetScalarList = SetScalarList(project, where, field, values)

  def getCreateMutaction(model: Model, args: CoolArgs, id: Id): CreateDataItem = {
    val scalarArguments = for {
      field      <- model.scalarNonListFields
      fieldValue <- args.getFieldValueAs[Any](field)
    } yield {
      ArgumentValue(field.name, fieldValue)
    }

    CreateDataItem(project, model, values = scalarArguments :+ ArgumentValue("id", id), originalArgs = Some(args))
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

  def getMutactionsForScalarLists(where: NodeSelector, args: CoolArgs): Vector[SetScalarList] = {
    val x = for {
      field  <- where.model.scalarListFields
      values <- args.subScalarList(field)
    } yield {
      values.values.nonEmpty.toOption {
        getSetScalarList(where, field, values.values)
      }
    }
    x.flatten.toVector
  }

  def getMutactionsForNestedMutation(args: CoolArgs,
                                     outerWhere: NodeSelector,
                                     triggeredFromCreate: Boolean,
                                     omitRelation: Option[Relation] = None): Seq[ClientSqlMutaction] = {

    val x = for {
      field          <- outerWhere.model.relationFields.filter(f => f.relation != omitRelation)
      subModel       = field.relatedModel_!(project.schema)
      nestedMutation = args.subNestedMutation(field, subModel)
      parentInfo     = ParentInfo(field, outerWhere)
    } yield {

      val checkMutactions = getMutactionsForWhereChecks(nestedMutation) ++ getMutactionsForConnectionChecks(subModel, nestedMutation, parentInfo)

      val mutactionsThatACreateCanTrigger = getMutactionsForNestedCreateMutation(subModel, nestedMutation, parentInfo) ++
        getMutactionsForNestedConnectMutation(nestedMutation, parentInfo)

      val otherMutactions = getMutactionsForNestedDisconnectMutation(nestedMutation, parentInfo) ++
        getMutactionsForNestedDeleteMutation(nestedMutation, parentInfo) ++
        getMutactionsForNestedUpdateMutation(nestedMutation, parentInfo) ++
        getMutactionsForNestedUpsertMutation(subModel, nestedMutation, parentInfo)

      val orderedMutactions = checkMutactions ++ mutactionsThatACreateCanTrigger ++ otherMutactions

      if (triggeredFromCreate && mutactionsThatACreateCanTrigger.isEmpty && field.isRequired) throw RelationIsRequired(field.name, outerWhere.model.name)
      orderedMutactions
    }
    x.flatten
  }

  def getMutactionsForWhereChecks(nestedMutation: NestedMutation): Seq[ClientSqlMutaction] = {
    nestedMutation.updates.map(update => VerifyWhere(project, update.where)) ++
      nestedMutation.deletes.map(delete => VerifyWhere(project, delete.where)) ++
      nestedMutation.connects.map(connect => VerifyWhere(project, connect.where)) ++
      nestedMutation.disconnects.map(disconnect => VerifyWhere(project, disconnect.where))
  }

  def getMutactionsForConnectionChecks(model: Model, nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.updates.map(update => VerifyConnection(project, parentInfo, update.where)) ++
      nestedMutation.deletes.map(delete => VerifyConnection(project, parentInfo, delete.where)) ++
      nestedMutation.disconnects.map(disconnect => VerifyConnection(project, parentInfo, disconnect.where))
  }

  def getMutactionsForNestedCreateMutation(model: Model, nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.creates.flatMap { create =>
      val id          = createCuid()
      val where       = NodeSelector.forId(model, id)
      val createItem  = getCreateMutaction(model, create.data, id)
      val connectItem = AddDataItemToManyRelationByUniqueField(project, parentInfo, where)
      val scalarLists = getMutactionsForScalarLists(where, create.data)

      List(createItem, connectItem) ++ scalarLists ++ getMutactionsForNestedMutation(create.data,
                                                                                     where,
                                                                                     triggeredFromCreate = true,
                                                                                     omitRelation = parentInfo.field.relation)
    }
  }

  def getMutactionsForNestedConnectMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.connects.map(connect => AddDataItemToManyRelationByUniqueField(project, parentInfo, connect.where))
  }

  def getMutactionsForNestedDisconnectMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.disconnects.map(disconnect => RemoveDataItemFromManyRelationByUniqueField(project, parentInfo, disconnect.where))
  }

  def getMutactionsForNestedDeleteMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.deletes.map(delete => DeleteDataItemByUniqueFieldIfInRelationWith(project, parentInfo, delete.where))
  }

  def getMutactionsForNestedUpdateMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.updates.flatMap { update =>
      val updateMutaction = UpdateDataItemByUniqueFieldIfInRelationWith(project, parentInfo, update.where, update.data)
      val scalarLists     = getMutactionsForScalarLists(update.where, update.data)
      List(updateMutaction) ++ scalarLists ++ getMutactionsForNestedMutation(update.data, update.where, triggeredFromCreate = false)
    }
  }

  // we need to rethink this thoroughly, we need to prevent both branches of executing their nested mutations && scalarlist mutations at the same time
  // scalarlists are not in here atm

  def getMutactionsForNestedUpsertMutation(model: Model, nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.upserts.flatMap { upsert =>
      val upsertItem    = UpsertDataItemIfInRelationWith(project, parentInfo, upsert.where, upsert.create, upsert.update)
      val idWhere       = NodeSelector.forId(model, upsertItem.idOfNewItem)
      val addToRelation = AddDataItemToManyRelationByUniqueField(project, parentInfo, idWhere)
      Vector(upsertItem, addToRelation) ++ getMutactionsForNestedMutation(upsert.update, upsert.where, triggeredFromCreate = false) ++
        getMutactionsForNestedMutation(upsert.create, idWhere, triggeredFromCreate = true)
    }
  }

  private def checkIfRemovalWouldFailARequiredRelation(field: Field, fromId: String, project: Project): Option[InvalidInputClientSqlMutaction] = {
    val isInvalid = () => dataResolver.resolveByRelation(fromField = field, fromModelId = fromId, args = None).map(_.items.nonEmpty)

    runRequiredRelationCheckWithInvalidFunction(field, isInvalid)
  }

  private def runRequiredRelationCheckWithInvalidFunction(field: Field, isInvalid: () => Future[Boolean]): Option[InvalidInputClientSqlMutaction] = {
    field.relatedField(project.schema).flatMap { relatedField =>
      val relatedModel = field.relatedModel_!(project.schema)

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

object NestedMutation {
  def empty = NestedMutation(Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty)
}

case class CreateOne(data: CoolArgs)
case class UpdateOne(where: NodeSelector, data: CoolArgs)
case class UpsertOne(where: NodeSelector, create: CoolArgs, update: CoolArgs)
case class DeleteOne(where: NodeSelector)
case class ConnectOne(where: NodeSelector)
case class DisconnectOne(where: NodeSelector)

case class ScalarListSet(values: Vector[Any])
