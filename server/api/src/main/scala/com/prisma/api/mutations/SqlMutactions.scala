package com.prisma.api.mutations
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlMutaction}
import com.prisma.api.database.mutactions.mutactions._
import com.prisma.api.database.{DataItem, DataResolver, DatabaseMutationBuilder}
import com.prisma.api.schema.APIErrors.RelationIsRequired
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Field, Model, Project, Relation}
import com.prisma.utils.boolean.BooleanUtils._
import cool.graph.cuid.Cuid.createCuid
import slick.dbio.{DBIOAction, Effect, NoStream}

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

  def getMutactionsForDelete(where: NodeSelector, previousValues: DataItem, id: String): List[ClientSqlMutaction] = {
    val requiredRelationViolations     = where.model.relationFields.flatMap(field => checkIfRemovalWouldFailARequiredRelation(project, ParentInfo(field, where)))
    val removeFromConnectionMutactions = where.model.relationFields.map(field => RemoveRelationRowByParentInfo(project, ParentInfo(field, where)))
    val deleteItemMutaction            = DeleteDataItem(project, where, previousValues, id)

    requiredRelationViolations ++ removeFromConnectionMutactions ++ List(deleteItemMutaction)
  }

  def getMutactionsForUpdate(where: NodeSelector, args: CoolArgs, id: Id, previousValues: DataItem): Vector[ClientSqlMutaction] = {
    val updateMutaction = getUpdateMutactions(where, args, id, previousValues)
    val whereFieldValue = args.raw.get(where.field.name)
    val updatedWhere    = if (whereFieldValue.isDefined) where.updateValue(whereFieldValue.get) else where
    val nested          = getMutactionsForNestedMutation(args, updatedWhere, triggeredFromCreate = false)

    updateMutaction ++ nested
  }

  def getMutactionsForCreate(model: Model, args: CoolArgs, id: Id): Vector[ClientSqlMutaction] = {
    val where            = NodeSelector.forId(model, id)
    val createMutactions = getCreateMutactions(where, args)
    val nestedMutactions = getMutactionsForNestedMutation(args, where, triggeredFromCreate = true)

    createMutactions ++ nestedMutactions
  }

  // we need to rethink this thoroughly, we need to prevent both branches of executing their nested mutations
  def getMutactionsForUpsert(outerWhere: NodeSelector, createWhere: NodeSelector, updateWhere: NodeSelector, allArgs: CoolArgs): List[ClientSqlMutaction] = {
    val upsertMutaction = UpsertDataItem(project, outerWhere, createWhere, updateWhere, allArgs, dataResolver)

//    val updateNested = getMutactionsForNestedMutation(allArgs.updateArgumentsAsCoolArgs, updatedOuterWhere, triggeredFromCreate = false)
//    val createNested = getMutactionsForNestedMutation(allArgs.createArgumentsAsCoolArgs, createWhere, triggeredFromCreate = true)

    List(upsertMutaction) //++ updateNested ++ createNested
  }

  def getCreateMutactions(where: NodeSelector, args: CoolArgs): Vector[ClientSqlMutaction] = {
    val createNonLists = CreateDataItem(project, where, args)
    val createLists    = getMutactionsForScalarLists(where, args)

    createNonLists +: createLists
  }

  def getUpdateMutactions(where: NodeSelector, args: CoolArgs, id: Id, previousValues: DataItem): Vector[ClientSqlMutaction] = {
    val updateNonLists =
      UpdateDataItem(
        project = project,
        model = where.model,
        id = id,
        args = args.nonListScalarArguments(where.model),
        previousValues = previousValues,
        itemExists = true
      )

    val updateLists = getMutactionsForScalarLists(where, args)

    updateNonLists +: updateLists
  }

  def getSetScalarList(where: NodeSelector, field: Field, values: Vector[Any]): SetScalarList = SetScalarList(project, where, field, values)
  def getSetScalarListActionsForUpsert(where: NodeSelector, field: Field, values: Vector[Any]) =
    DatabaseMutationBuilder.setScalarList(project.id, where, field.name, values)

  def getMutactionsForScalarLists(where: NodeSelector, args: CoolArgs): Vector[ClientSqlDataChangeMutaction] = {
    val x = for {
      field  <- where.model.scalarListFields
      values <- args.subScalarList(field)
    } yield {
      values.values.isEmpty match {
        case true  => SetScalarListToEmpty(project, where, field)
        case false => getSetScalarList(where, field, values.values)
      }
    }
    x.toVector
  }

  def getDbActionsForUpsertScalarLists(where: NodeSelector, args: CoolArgs): Vector[DBIOAction[Any, NoStream, Effect]] = {
    val x = for {
      field  <- where.model.scalarListFields
      values <- args.subScalarList(field)
    } yield {
      values.values.isEmpty match {
        case true  => DatabaseMutationBuilder.setScalarListToEmpty(project.id, where, field.name)
        case false => getSetScalarListActionsForUpsert(where, field, values.values)
      }
    }
    x.toVector
  }

  //filter for duplicates here? multiple identical where checks for example?
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

      val mutactionsThatACreateCanTrigger = getMutactionsForNestedCreateMutation(subModel, nestedMutation, parentInfo, triggeredFromCreate) ++
        getMutactionsForNestedConnectMutation(nestedMutation, parentInfo, triggeredFromCreate)

      val otherMutactions = getMutactionsForNestedDisconnectMutation(nestedMutation, parentInfo) ++
        getMutactionsForNestedDeleteMutation(nestedMutation, parentInfo) ++
        getMutactionsForNestedUpdateMutation(nestedMutation, parentInfo) ++
        getMutactionsForNestedUpsertMutation(nestedMutation, parentInfo)

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

  def getMutactionsForNestedCreateMutation(model: Model,
                                           nestedMutation: NestedMutation,
                                           parentInfo: ParentInfo,
                                           triggeredFromCreate: Boolean): Seq[ClientSqlMutaction] = {
    nestedMutation.creates.flatMap { create =>
      val where            = NodeSelector.forId(model, createCuid())
      val createMutactions = getCreateMutactions(where, create.data)
      val connectItem      = List(NestedCreateRelationMutaction(project, parentInfo, where, triggeredFromCreate))

      createMutactions ++ connectItem ++ getMutactionsForNestedMutation(create.data,
                                                                        where,
                                                                        triggeredFromCreate = true,
                                                                        omitRelation = parentInfo.field.relation)
    }
  }

  def getMutactionsForNestedConnectMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo, topIsCreate: Boolean): Seq[ClientSqlMutaction] = {
    nestedMutation.connects.map(connect => NestedConnectRelationMutaction(project, parentInfo, connect.where, topIsCreate))
  }

  def getMutactionsForNestedDisconnectMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.disconnects.map(disconnect => NestedDisconnectRelationMutaction(project, parentInfo, disconnect.where))
  }

  def getMutactionsForNestedDeleteMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.deletes.flatMap(delete =>
      List(NestedDeleteRelationMutaction(project, parentInfo, delete.where), DeleteDataItemNested(project, delete.where)))
  }

  def getMutactionsForNestedUpdateMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.updates.flatMap { update =>
      val updateMutaction = List(UpdateDataItemByUniqueFieldIfInRelationWith(project, parentInfo, update.where, update.data))
      val whereFieldValue = update.data.raw.get(update.where.field.name)
      val updatedWhere    = if (whereFieldValue.isDefined) update.where.updateValue(whereFieldValue.get) else update.where
      val scalarLists     = getMutactionsForScalarLists(updatedWhere, update.data)

      updateMutaction ++ scalarLists ++ getMutactionsForNestedMutation(update.data, updatedWhere, triggeredFromCreate = false)
    }
  }

  // we need to rethink this thoroughly, we need to prevent both branches of executing their nested mutations
  // generate default value in create case
  def getMutactionsForNestedUpsertMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo): Seq[ClientSqlMutaction] = {
    nestedMutation.upserts.flatMap { upsert =>
      val id                = createCuid()
      val createWhere       = NodeSelector.forId(upsert.where.model, id)
      val createArgsWithId  = CoolArgs(upsert.create.raw + ("id" -> id))
      val whereFieldValue   = upsert.update.raw.get(upsert.where.field.name)
      val updatedWhere      = if (whereFieldValue.isDefined) upsert.where.updateValue(whereFieldValue.get) else upsert.where
      val scalarListsCreate = getDbActionsForUpsertScalarLists(createWhere, createArgsWithId)
      val scalarListsUpdate = getDbActionsForUpsertScalarLists(updatedWhere, upsert.update)
      val upsertItem =
        UpsertDataItemIfInRelationWith(project, parentInfo, upsert.where, createWhere, createArgsWithId, upsert.update, scalarListsCreate, scalarListsUpdate)
      val addToRelation = AddDataItemToManyRelationByUniqueField(project, parentInfo, createWhere)
      Vector(upsertItem, addToRelation) //++ getMutactionsForNestedMutation(upsert.update, upsert.where, triggeredFromCreate = false) ++
    //getMutactionsForNestedMutation(upsert.create, createWhere, triggeredFromCreate = true)
    }
  }

  private def checkIfRemovalWouldFailARequiredRelation(project: Project, parentInfo: ParentInfo): Option[InvalidInputClientSqlMutaction] = {
    val isInvalid = () => dataResolver.resolveByUnique(parentInfo.where).map(_.nonEmpty)

    runRequiredRelationCheckWithInvalidFunction(parentInfo.field, isInvalid)
  }

  private def runRequiredRelationCheckWithInvalidFunction(field: Field, isInvalid: () => Future[Boolean]): Option[InvalidInputClientSqlMutaction] = {
    field.relatedField(project.schema).flatMap { relatedField =>
      val relatedModel              = field.relatedModel_!(project.schema)
      val error: RelationIsRequired = RelationIsRequired(fieldName = relatedField.name, typeName = relatedModel.name)

      (relatedField.isRequired && !relatedField.isList).toOption {
        InvalidInputClientSqlMutaction(error, isInvalid = isInvalid)
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
