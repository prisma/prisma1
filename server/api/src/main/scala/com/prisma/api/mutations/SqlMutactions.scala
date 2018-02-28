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
    val nested          = getMutactionsForNestedMutation(args, path.updatedRoot(args), triggeredFromCreate = false)

    updateMutaction ++ nested
  }

  def getMutactionsForCreate(model: Model, args: CoolArgs, id: Id): Seq[ClientSqlMutaction] = report {
    val path             = Path.empty(NodeSelector.forId(model, id))
    val createMutactions = CreateDataItem(project, path, args) +: getMutactionsForScalarLists(path, args)
    val nestedMutactions = getMutactionsForNestedMutation(args, path, triggeredFromCreate = true)

    createMutactions ++ nestedMutactions
  }

  // we need to rethink this thoroughly, we need to prevent both branches of executing their nested mutations
  def getMutactionsForUpsert(path: Path, createWhere: NodeSelector, updatedWhere: NodeSelector, allArgs: CoolArgs): Seq[ClientSqlMutaction] =
    report {
      val upsertMutaction = UpsertDataItem(project, path, createWhere, updatedWhere, allArgs, dataResolver)

//    val updateNested = getMutactionsForNestedMutation(allArgs.updateArgumentsAsCoolArgs, updatedOuterWhere, triggeredFromCreate = false)
//    val createNested = getMutactionsForNestedMutation(allArgs.createArgumentsAsCoolArgs, createWhere, triggeredFromCreate = true)

      List(upsertMutaction) //++ updateNested ++ createNested
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

    val updateLists = getMutactionsForScalarLists(path, args)

    updateNonLists +: updateLists
  }

  def getSetScalarList(path: Path, field: Field, values: Vector[Any]): SetScalarList = SetScalarList(project, path, field, values)

  def getMutactionsForScalarLists(path: Path, args: CoolArgs): Vector[ClientSqlDataChangeMutaction] = {
    val x = for {
      field  <- path.lastModel.scalarListFields
      values <- args.subScalarList(field)
    } yield {
      values.values.isEmpty match {
        case true  => SetScalarListToEmpty(project, path, field)
        case false => getSetScalarList(path, field, values.values)
      }
    }
    x.toVector
  }

  def getDbActionsForUpsertScalarLists(path: Path, args: CoolArgs): Vector[DBIOAction[Any, NoStream, Effect]] = {
    val x = for {
      field  <- path.lastModel.scalarListFields
      values <- args.subScalarList(field)
    } yield {
      values.values.isEmpty match {
        case true  => DatabaseMutationBuilder.setScalarListToEmptyPath(project.id, path, field.name)
        case false => DatabaseMutationBuilder.setScalarListPath(project.id, path, field.name, values.values)
      }
    }
    x.toVector
  }

  // Todo filter for duplicates here? multiple identical where checks for example?
  def getMutactionsForNestedMutation(args: CoolArgs, path: Path, triggeredFromCreate: Boolean): Seq[ClientSqlMutaction] = {

    val x = for {
      field          <- path.lastModel.relationFields.filter(f => f.relation != path.lastRelation)
      subModel       = field.relatedModel_!(project.schema)
      nestedMutation = args.subNestedMutation(field, subModel)
      edge           = ModelEdge(path.lastModel, field, field.relatedModel(project.schema).get, field.relatedField(project.schema), field.relation.get)
      extendedPath   = path.append(edge)
    } yield {

      val checkMutactions = getMutactionsForWhereChecks(nestedMutation) ++ getMutactionsForConnectionChecks(subModel, nestedMutation, extendedPath)

      val mutactionsThatACreateCanTrigger = getMutactionsForNestedCreateMutation(subModel, nestedMutation, extendedPath, triggeredFromCreate) ++
        getMutactionsForNestedConnectMutation(nestedMutation, extendedPath, triggeredFromCreate)

      val otherMutactions = getMutactionsForNestedDisconnectMutation(nestedMutation, extendedPath) ++
        getMutactionsForNestedDeleteMutation(nestedMutation, extendedPath) ++
        getMutactionsForNestedUpdateMutation(nestedMutation, extendedPath) ++
        getMutactionsForNestedUpsertMutation(nestedMutation, extendedPath)

      if (triggeredFromCreate && mutactionsThatACreateCanTrigger.isEmpty && field.isRequired) throw RelationIsRequired(field.name, path.lastModel.name)

      checkMutactions ++ mutactionsThatACreateCanTrigger ++ otherMutactions
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
      val specifiedPath    = path.lastEdgeToNodeEdge(NodeSelector.forId(model, createCuid()))
      val createMutactions = List(CreateDataItem(project, specifiedPath, create.data))
      val listMutactions   = getMutactionsForScalarLists(specifiedPath, create.data)
      val connectItem      = List(NestedCreateRelationMutaction(project, specifiedPath, triggeredFromCreate))

      createMutactions ++ connectItem ++ listMutactions ++ getMutactionsForNestedMutation(create.data, specifiedPath, triggeredFromCreate = true)
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
      val specifiedPath             = path.lastEdgeToNodeEdgeIfNecessary(delete)
      val cascadingDeleteMutactions = generateCascadingDeleteMutactions(specifiedPath)
      cascadingDeleteMutactions ++ List(DeleteRelationMutaction(project, specifiedPath), DeleteDataItemNested(project, specifiedPath))
    }
  }

  def getMutactionsForNestedUpdateMutation(nestedMutation: NestedMutation, path: Path): Seq[ClientSqlMutaction] = {
    nestedMutation.updates.collect {
      case update: UpdateByWhere =>
        val updateMutaction = List(UpdateDataItemByUniqueFieldIfInRelationWith(project, path.lastEdgeToNodeEdge(update.where), update.data))
        val updatedWhere    = currentWhere(update.where, update.data)
        val scalarLists     = getMutactionsForScalarLists(path.lastEdgeToNodeEdge(updatedWhere), update.data)

        updateMutaction ++ scalarLists ++ getMutactionsForNestedMutation(update.data, path.lastEdgeToNodeEdge(updatedWhere), triggeredFromCreate = false)
      case update: UpdateByRelation =>
        val updateMutaction = List(UpdateDataItemIfInRelationWith(project, path, update.data))
        val scalarLists     = getMutactionsForScalarLists(path, update.data)

        updateMutaction ++ scalarLists ++ getMutactionsForNestedMutation(update.data, path, triggeredFromCreate = false)
    }.flatten
  }

  // we need to rethink this thoroughly, we need to prevent both branches of executing their nested mutations
  // generate default value in create case
  def getMutactionsForNestedUpsertMutation(nestedMutation: NestedMutation, path: Path): Seq[ClientSqlMutaction] = {
    nestedMutation.upserts.flatMap { upsert =>
      val id                = createCuid()
      val createWhere       = NodeSelector.forId(path.lastModel, id)
      val createArgsWithId  = CoolArgs(upsert.create.raw + ("id" -> id))
      val scalarListsCreate = getDbActionsForUpsertScalarLists(path.lastEdgeToNodeEdge(createWhere), createArgsWithId)

      val upsertItem = upsert match {
        case upsert: UpsertByWhere =>
          val scalarListsUpdate = getDbActionsForUpsertScalarLists(path.lastEdgeToNodeEdge(currentWhere(upsert.where, upsert.update)), upsert.update)
          UpsertDataItemIfInRelationWith(project,
                                         path.lastEdgeToNodeEdge(upsert.where),
                                         createWhere,
                                         createArgsWithId,
                                         upsert.update,
                                         scalarListsCreate,
                                         scalarListsUpdate)

        case upsert: UpsertByRelation =>
          val scalarListsUpdate = getDbActionsForUpsertScalarLists(path, upsert.update)
          UpsertDataItemIfInRelationWith(project, path, createWhere, createArgsWithId, upsert.update, scalarListsCreate, scalarListsUpdate)
      }

      Vector(upsertItem) //++ getMutactionsForNestedMutation(upsert.update, upsert.where, triggeredFromCreate = false) ++
    //getMutactionsForNestedMutation(upsert.create, createWhere, triggeredFromCreate = true)
    }
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
