package com.prisma.api.mutations
import com.prisma.api.ApiMetrics
import com.prisma.api.database.mutactions.mutactions._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlMutaction}
import com.prisma.api.database.{DataItem, DataResolver, DatabaseMutationBuilder}
import com.prisma.api.mutations.mutations.CascadingDeletes.{ModelEdge, Path, collectCascadingPaths}
import com.prisma.api.schema.APIErrors.RelationIsRequired
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Field, Model, Relation}
import cool.graph.cuid.Cuid.createCuid
import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.collection.immutable.Seq

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

  def report(mutactions: Seq[ClientSqlMutaction]): Seq[ClientSqlMutaction] = {
    ApiMetrics.mutactionCount.incBy(mutactions.size, project.id)
    mutactions
  }

  def getMutactionsForDelete(path: Path, previousValues: DataItem, id: String): Seq[ClientSqlMutaction] = report {
    generateCascadingDeleteMutactions(path) ++ List(DeleteRelationMutaction(project, path), DeleteDataItem(project, path, previousValues, id))
  }

  def getMutactionsForUpdate(path: Path, args: CoolArgs, id: Id, previousValues: DataItem): Seq[ClientSqlMutaction] = report {
    val updateMutaction = getUpdateMutactions(path, args, id, previousValues)
    val nested          = getMutactionsForNestedMutation(args, currentWhere(path.root, args), path.updatedRoot(args), triggeredFromCreate = false)

    updateMutaction ++ nested
  }

  def getMutactionsForCreate(model: Model, args: CoolArgs, id: Id): Seq[ClientSqlMutaction] = report {
    val where            = NodeSelector.forId(model, id)
    val createMutactions = getCreateMutactions(where, args)
    val nestedMutactions = getMutactionsForNestedMutation(args, where, Path.empty(where), triggeredFromCreate = true)

    createMutactions ++ nestedMutactions
  }

  // we need to rethink this thoroughly, we need to prevent both branches of executing their nested mutations
  def getMutactionsForUpsert(outerWhere: NodeSelector, createWhere: NodeSelector, updateWhere: NodeSelector, allArgs: CoolArgs): Seq[ClientSqlMutaction] =
    report {
      val upsertMutaction = UpsertDataItem(project, outerWhere, createWhere, updateWhere, allArgs, dataResolver)

//    val updateNested = getMutactionsForNestedMutation(allArgs.updateArgumentsAsCoolArgs, updatedOuterWhere, triggeredFromCreate = false)
//    val createNested = getMutactionsForNestedMutation(allArgs.createArgumentsAsCoolArgs, createWhere, triggeredFromCreate = true)

      List(upsertMutaction) //++ updateNested ++ createNested
    }

  def getCreateMutactions(where: NodeSelector, args: CoolArgs): Vector[ClientSqlMutaction] = {
    CreateDataItem(project, where, args) +: getMutactionsForScalarLists(where, args)
  }

  def getUpdateMutactions(path: Path, args: CoolArgs, id: Id, previousValues: DataItem): Vector[ClientSqlMutaction] = {
    val updateNonLists =
      UpdateDataItem(
        project = project,
        model = path.lastModel,
        id = id,
        args = args.nonListScalarArguments(path.lastModel),
        previousValues = previousValues,
        itemExists = true
      )

    val updateLists = getMutactionsForScalarLists(path.root, args)

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

  // Todo filter for duplicates here? multiple identical where checks for example?
  def getMutactionsForNestedMutation(args: CoolArgs, outerWhere: NodeSelector, path: Path, triggeredFromCreate: Boolean): Seq[ClientSqlMutaction] = {

    val x = for {
      field          <- path.lastModel.relationFields.filter(f => f.relation != path.lastRelation)
      subModel       = field.relatedModel_!(project.schema)
      nestedMutation = args.subNestedMutation(field, subModel)
      parentInfo     = ParentInfo(field, outerWhere)
      edge           = ModelEdge(path.lastModel, field, field.relatedModel(project.schema).get, field.relatedField(project.schema), field.relation.get)
      extendedPath   = path.append(edge)
    } yield {

      val checkMutactions = getMutactionsForWhereChecks(nestedMutation) ++ getMutactionsForConnectionChecks(subModel, nestedMutation, extendedPath)

      val mutactionsThatACreateCanTrigger = getMutactionsForNestedCreateMutation(subModel, nestedMutation, extendedPath, triggeredFromCreate) ++
        getMutactionsForNestedConnectMutation(nestedMutation, extendedPath, triggeredFromCreate)

      val otherMutactions = getMutactionsForNestedDisconnectMutation(nestedMutation, extendedPath) ++
        getMutactionsForNestedDeleteMutation(nestedMutation, extendedPath) ++
        getMutactionsForNestedUpdateMutation(nestedMutation, parentInfo, extendedPath) ++
        getMutactionsForNestedUpsertMutation(nestedMutation, parentInfo, extendedPath)

      val orderedMutactions = checkMutactions ++ mutactionsThatACreateCanTrigger ++ otherMutactions

      if (triggeredFromCreate && mutactionsThatACreateCanTrigger.isEmpty && field.isRequired) throw RelationIsRequired(field.name, outerWhere.model.name)
      orderedMutactions
    }
    x.flatten
  }

  def getMutactionsForWhereChecks(nestedMutation: NestedMutation): Seq[ClientSqlMutaction] = {
    nestedMutation.updates.collect { case update: UpdateByWhere               => VerifyWhere(project, update.where) } ++
      nestedMutation.deletes.collect { case delete: DeleteByWhere             => VerifyWhere(project, delete.where) } ++
      nestedMutation.connects.collect { case connect: ConnectByWhere          => VerifyWhere(project, connect.where) } ++
      nestedMutation.disconnects.collect { case disconnect: DisconnectByWhere => VerifyWhere(project, disconnect.where) }
  }

  def getMutactionsForConnectionChecks(model: Model, nestedMutation: NestedMutation, path: Path): Seq[ClientSqlMutaction] = {
    nestedMutation.updates.map(update => VerifyConnection(project, path.lastEdgeToNodeEdgeIfNecessary(update))) ++
      nestedMutation.deletes.map(delete => VerifyConnection(project, path.lastEdgeToNodeEdgeIfNecessary(delete))) ++
      nestedMutation.disconnects.map(disconnect => VerifyConnection(project, path.lastEdgeToNodeEdgeIfNecessary(disconnect)))
  }

  def getMutactionsForNestedCreateMutation(model: Model, nestedMutation: NestedMutation, path: Path, triggeredFromCreate: Boolean): Seq[ClientSqlMutaction] = {
    nestedMutation.creates.flatMap { create =>
      val where            = NodeSelector.forId(model, createCuid())
      val createMutactions = getCreateMutactions(where, create.data)
      val connectItem      = List(NestedCreateRelationMutaction(project, path.lastEdgeToNodeEdge(where), triggeredFromCreate))

      createMutactions ++ connectItem ++ getMutactionsForNestedMutation(create.data, where, path.lastEdgeToNodeEdge(where), triggeredFromCreate = true)
    }
  }

  def getMutactionsForNestedConnectMutation(nestedMutation: NestedMutation, path: Path, topIsCreate: Boolean): Seq[ClientSqlMutaction] = {
    nestedMutation.connects.map(connect => NestedConnectRelationMutaction(project, path.lastEdgeToNodeEdgeIfNecessary(connect), topIsCreate))
  }

  def getMutactionsForNestedDisconnectMutation(nestedMutation: NestedMutation, path: Path): Seq[ClientSqlMutaction] = {
    nestedMutation.disconnects.map(disconnect => NestedDisconnectRelationMutaction(project, path.lastEdgeToNodeEdgeIfNecessary(disconnect)))
  }

  def getMutactionsForNestedDeleteMutation(nestedMutation: NestedMutation, path: Path): Seq[ClientSqlMutaction] = {
    nestedMutation.deletes.flatMap { delete =>
      val updatedPath               = path.lastEdgeToNodeEdgeIfNecessary(delete)
      val cascadingDeleteMutactions = generateCascadingDeleteMutactions(updatedPath)
      cascadingDeleteMutactions ++ List(DeleteRelationMutaction(project, updatedPath), DeleteDataItemNested(project, updatedPath))
    }
  }

  def getMutactionsForNestedUpdateMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo, path: Path): Seq[ClientSqlMutaction] = {
    nestedMutation.updates.collect {
      case update: UpdateByWhere =>
        val updateMutaction = List(UpdateDataItemByUniqueFieldIfInRelationWith(project, parentInfo, update.where, update.data))
        val updatedWhere    = currentWhere(update.where, update.data)
        val scalarLists     = getMutactionsForScalarLists(updatedWhere, update.data)

        updateMutaction ++ scalarLists ++ getMutactionsForNestedMutation(update.data, updatedWhere, path, triggeredFromCreate = false)
      case update: UpdateByRelation =>
        val updateMutaction = List(UpdateDataItemIfInRelationWith(project, path, update.data))
//        val scalarLists     = getMutactionsForScalarLists(updatedWhere, update.data) todo

//        updateMutaction ++ scalarLists ++ getMutactionsForNestedMutation(update.data, updatedWhere, path, triggeredFromCreate = false)
        updateMutaction ++ getMutactionsForNestedMutation(update.data, NodeSelector.forId(path.lastModel, "TODO"), path, triggeredFromCreate = false)

    }.flatten
  }

  // we need to rethink this thoroughly, we need to prevent both branches of executing their nested mutations
  // generate default value in create case
  def getMutactionsForNestedUpsertMutation(nestedMutation: NestedMutation, parentInfo: ParentInfo, path: Path): Seq[ClientSqlMutaction] = {
    nestedMutation.upserts.collect {
      case upsert: UpsertByWhere =>
        val id                = createCuid()
        val createWhere       = NodeSelector.forId(upsert.where.model, id)
        val createArgsWithId  = CoolArgs(upsert.create.raw + ("id" -> id))
        val scalarListsCreate = getDbActionsForUpsertScalarLists(createWhere, createArgsWithId)
        val scalarListsUpdate = getDbActionsForUpsertScalarLists(currentWhere(upsert.where, upsert.update), upsert.update)
        val upsertItem =
          UpsertDataItemIfInRelationWith(project,
                                         parentInfo,
                                         upsert.where,
                                         createWhere,
                                         createArgsWithId,
                                         upsert.update,
                                         scalarListsCreate,
                                         scalarListsUpdate,
                                         path)

        Vector(upsertItem) //++ getMutactionsForNestedMutation(upsert.update, upsert.where, triggeredFromCreate = false) ++
      //getMutactionsForNestedMutation(upsert.create, createWhere, triggeredFromCreate = true)
    }.flatten
  }

  private def currentWhere(where: NodeSelector, args: CoolArgs) = {
    val whereFieldValue = args.raw.get(where.field.name)
    val updatedWhere    = whereFieldValue.map(where.updateValue).getOrElse(where)
    updatedWhere
  }

  def generateCascadingDeleteMutactions(startPoint: Path): List[ClientSqlMutaction] = {
    def getMutactionsForEdges(paths: List[Path]): List[ClientSqlMutaction] = {
      paths.filter(_.edges.length > startPoint.edges.length) match {
        case Nil =>
          List.empty

        case pathsList =>
          val maxPathLength     = pathsList.map(_.edges.length).max
          val longestPaths      = pathsList.filter(_.edges.length == maxPathLength)
          val longestMutactions = longestPaths.map(CascadingDeleteRelationMutactions(project, _))
          val shortenedPaths    = longestPaths.map(_.removeLastEdge)
          val newPaths          = pathsList.filter(_.edges.length < maxPathLength) ++ shortenedPaths

          longestMutactions ++ getMutactionsForEdges(newPaths)
      }
    }

    val paths: List[Path] = collectCascadingPaths(project, startPoint)
    getMutactionsForEdges(paths)
  }
}

case class NestedMutation(
    creates: Vector[CreateOne],
    updates: Vector[UpdateOne],
    upserts: Vector[UpsertOne],
    deletes: Vector[DeleteOne],
    connects: Vector[ConnectByWhere],
    disconnects: Vector[DisconnectOne]
)

object NestedMutation {
  def empty = NestedMutation(Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty)
}

trait NestedMutationBase
trait NestedWhere { def where: NodeSelector }
case class CreateOne(data: CoolArgs) extends NestedMutationBase

case class ConnectByWhere(where: NodeSelector) extends NestedMutationBase with NestedWhere

trait UpdateOne                                               extends NestedMutationBase { def data: CoolArgs }
case class UpdateByRelation(data: CoolArgs)                   extends UpdateOne
case class UpdateByWhere(where: NodeSelector, data: CoolArgs) extends UpdateOne with NestedWhere

trait UpsertOne                                                                   extends NestedMutationBase { def create: CoolArgs; def update: CoolArgs }
case class UpsertByRelation(create: CoolArgs, update: CoolArgs)                   extends UpsertOne
case class UpsertByWhere(where: NodeSelector, create: CoolArgs, update: CoolArgs) extends UpsertOne with NestedWhere

trait DeleteOne                               extends NestedMutationBase
case class DeleteByRelation(boolean: Boolean) extends DeleteOne
case class DeleteByWhere(where: NodeSelector) extends DeleteOne with NestedWhere

trait DisconnectOne                               extends NestedMutationBase
case class DisconnectByRelation(boolean: Boolean) extends DisconnectOne
case class DisconnectByWhere(where: NodeSelector) extends DisconnectOne with NestedWhere

case class ScalarListSet(values: Vector[Any])
