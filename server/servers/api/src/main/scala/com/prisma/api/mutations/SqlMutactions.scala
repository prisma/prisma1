package com.prisma.api.mutations
import com.prisma.api.ApiMetrics
import com.prisma.api.database.mutactions.mutactions._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlMutaction}
import com.prisma.api.database.{DataItem, DataResolver, DatabaseMutationBuilder}
import com.prisma.api.mutations.mutations.CascadingDeletes.{ModelEdge, Path, collectCascadingPaths}
import com.prisma.api.schema.APIErrors.RelationIsRequired
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Field, Model, Project}
import cool.graph.cuid.Cuid.createCuid
import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.collection.immutable.Seq

case class CreateMutactionsResult(createMutaction: CreateDataItem,
                                  scalarListMutactions: Vector[ClientSqlMutaction],
                                  nestedMutactions: Seq[ClientSqlMutaction]) {
  def allMutactions: Vector[ClientSqlMutaction] = Vector(createMutaction) ++ scalarListMutactions ++ nestedMutactions
}

case class SqlMutactions(dataResolver: DataResolver) {
  val project: Project = dataResolver.project

  def report(mutactions: Seq[ClientSqlMutaction]): Seq[ClientSqlMutaction] = {
    ApiMetrics.mutactionCount.incBy(mutactions.size, project.id)
    mutactions
  }

  def getMutactionsForDelete(path: Path, previousValues: DataItem, id: String): Seq[ClientSqlMutaction] = report {
    List(VerifyWhere(project, path.root)) ++ generateCascadingDeleteMutactions(path) ++ List(DeleteRelationMutaction(project, path), DeleteDataItem(project, path, previousValues, id))
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
        case true  => DatabaseMutationBuilder.setScalarListToEmpty(project.id, path, field.name)
        case false => DatabaseMutationBuilder.setScalarList(project.id, path, field.name, values.values)
      }
    }
    x.toVector
  }

  // Todo filter for duplicates here? multiple identical where checks for example?
  def getMutactionsForNestedMutation(args: CoolArgs, path: Path, triggeredFromCreate: Boolean): Seq[ClientSqlMutaction] = {

    val x = for {
      field           <- path.lastModel.relationFields.filter(f => f.relation != path.lastRelation) //todo move into path
      subModel        = field.relatedModel_!(project.schema)
      nestedMutations = args.subNestedMutation(field, subModel)
    } yield {

      val checkMutactions = getMutactionsForWhereChecks(nestedMutations) ++ getMutactionsForConnectionChecks(subModel, nestedMutations, path, field)

      val mutactionsThatACreateCanTrigger = getMutactionsForNestedCreateMutation(subModel, nestedMutations, path, field, triggeredFromCreate) ++
        getMutactionsForNestedConnectMutation(nestedMutations, path, field, triggeredFromCreate)

      val otherMutactions = getMutactionsForNestedDisconnectMutation(nestedMutations, path, field) ++
        getMutactionsForNestedDeleteMutation(nestedMutations, path, field) ++
        getMutactionsForNestedUpdateMutation(nestedMutations, path, field) ++
        getMutactionsForNestedUpsertMutation(nestedMutations, path, field)

      if (triggeredFromCreate && mutactionsThatACreateCanTrigger.isEmpty && field.isRequired) throw RelationIsRequired(field.name, path.lastModel.name)

      checkMutactions ++ mutactionsThatACreateCanTrigger ++ otherMutactions
    }
    x.flatten
  }

  def getMutactionsForWhereChecks(nestedMutation: NestedMutations): Seq[ClientSqlMutaction] = {
    (nestedMutation.updates ++ nestedMutation.deletes ++ nestedMutation.connects ++ nestedMutation.disconnects).collect {
      case x: NestedWhere => VerifyWhere(project, x.where)
    }
  }

  def getMutactionsForConnectionChecks(model: Model, nestedMutation: NestedMutations, path: Path, field: Field): Seq[ClientSqlMutaction] = {
    (nestedMutation.updates ++ nestedMutation.deletes ++ nestedMutation.disconnects).map(x => VerifyConnection(project, path.extend(project, field, x)))
  }

  def getMutactionsForNestedCreateMutation(model: Model,
                                           nestedMutation: NestedMutations,
                                           path: Path,
                                           field: Field,
                                           triggeredFromCreate: Boolean): Seq[ClientSqlMutaction] = {
    nestedMutation.creates.flatMap { create =>
      val extendedPath     = path.extend(project, field, create).lastEdgeToNodeEdge(NodeSelector.forId(model, createCuid()))
      val createMutactions = List(CreateDataItem(project, extendedPath, create.data))
      val listMutactions   = getMutactionsForScalarLists(extendedPath, create.data)
      val connectItem      = List(NestedCreateRelationMutaction(project, extendedPath, triggeredFromCreate))

      createMutactions ++ connectItem ++ listMutactions ++ getMutactionsForNestedMutation(create.data, extendedPath, triggeredFromCreate = true)
    }
  }

  def getMutactionsForNestedConnectMutation(nestedMutation: NestedMutations, path: Path, field: Field, topIsCreate: Boolean): Seq[ClientSqlMutaction] = {
    nestedMutation.connects.map(connect => NestedConnectRelationMutaction(project, path.extend(project, field, connect), topIsCreate))
  }

  def getMutactionsForNestedDisconnectMutation(nestedMutation: NestedMutations, path: Path, field: Field): Seq[ClientSqlMutaction] = {
    nestedMutation.disconnects.map(disconnect => NestedDisconnectRelationMutaction(project, path.extend(project, field, disconnect)))
  }

  def getMutactionsForNestedDeleteMutation(nestedMutation: NestedMutations, path: Path, field: Field): Seq[ClientSqlMutaction] = {
    nestedMutation.deletes.flatMap { delete =>
      val extendedPath              = path.extend(project, field, delete)
      val cascadingDeleteMutactions = generateCascadingDeleteMutactions(extendedPath)
      cascadingDeleteMutactions ++ List(DeleteRelationMutaction(project, extendedPath), DeleteDataItemNested(project, extendedPath))
    }
  }

  def getMutactionsForNestedUpdateMutation(nestedMutation: NestedMutations, path: Path, field: Field): Seq[ClientSqlMutaction] = {
    nestedMutation.updates.flatMap { update =>
      val extendedPath = path.extend(project, field, update)
      val updatedPath = update match {
        case x: UpdateByWhere    => extendedPath.lastEdgeToNodeEdge(currentWhere(x.where, x.data))
        case _: UpdateByRelation => extendedPath
      }
      val updateMutaction = List(UpdateDataItemByUniqueFieldIfInRelationWith(project, extendedPath, update.data))
      val scalarLists     = getMutactionsForScalarLists(updatedPath, update.data)

      updateMutaction ++ scalarLists ++ getMutactionsForNestedMutation(update.data, updatedPath, triggeredFromCreate = false)
    }
  }

  // we need to rethink this thoroughly, we need to prevent both branches of executing their nested mutations
  // generate default value in create case
  def getMutactionsForNestedUpsertMutation(nestedMutation: NestedMutations, path: Path, field: Field): Seq[ClientSqlMutaction] = {
    nestedMutation.upserts.flatMap { upsert =>
      val extendedPath      = path.extend(project, field, upsert)
      val id                = createCuid()
      val createWhere       = NodeSelector.forId(extendedPath.lastModel, id)
      val createArgsWithId  = CoolArgs(upsert.create.raw + ("id" -> id))
      val scalarListsCreate = getDbActionsForUpsertScalarLists(extendedPath.lastEdgeToNodeEdge(createWhere), createArgsWithId)

      val upsertItem = upsert match {
        case upsert: UpsertByWhere =>
          val scalarListsUpdate = getDbActionsForUpsertScalarLists(extendedPath.lastEdgeToNodeEdge(currentWhere(upsert.where, upsert.update)), upsert.update)
          UpsertDataItemIfInRelationWith(project,
                                         extendedPath.lastEdgeToNodeEdge(upsert.where),
                                         createWhere,
                                         createArgsWithId,
                                         upsert.update,
                                         scalarListsCreate,
                                         scalarListsUpdate)

        case upsert: UpsertByRelation =>
          val scalarListsUpdate = getDbActionsForUpsertScalarLists(extendedPath, upsert.update)
          UpsertDataItemIfInRelationWith(project, extendedPath, createWhere, createArgsWithId, upsert.update, scalarListsCreate, scalarListsUpdate)
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

case class NestedMutations(
    creates: Vector[CreateOne],
    updates: Vector[UpdateOne],
    upserts: Vector[UpsertOne],
    deletes: Vector[DeleteOne],
    connects: Vector[ConnectByWhere],
    disconnects: Vector[DisconnectOne]
)

object NestedMutations {
  def empty = NestedMutations(Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty)
}

sealed trait NestedMutation
sealed trait NestedWhere { def where: NodeSelector }
case class CreateOne(data: CoolArgs) extends NestedMutation

case class ConnectByWhere(where: NodeSelector) extends NestedMutation with NestedWhere

sealed trait UpdateOne                                        extends NestedMutation { def data: CoolArgs }
case class UpdateByRelation(data: CoolArgs)                   extends UpdateOne
case class UpdateByWhere(where: NodeSelector, data: CoolArgs) extends UpdateOne with NestedWhere

sealed trait UpsertOne                                                            extends NestedMutation { def create: CoolArgs; def update: CoolArgs }
case class UpsertByRelation(create: CoolArgs, update: CoolArgs)                   extends UpsertOne
case class UpsertByWhere(where: NodeSelector, create: CoolArgs, update: CoolArgs) extends UpsertOne with NestedWhere

sealed trait DeleteOne                        extends NestedMutation
case class DeleteByRelation(boolean: Boolean) extends DeleteOne
case class DeleteByWhere(where: NodeSelector) extends DeleteOne with NestedWhere

sealed trait DisconnectOne                        extends NestedMutation
case class DisconnectByRelation(boolean: Boolean) extends DisconnectOne
case class DisconnectByWhere(where: NodeSelector) extends DisconnectOne with NestedWhere

case class ScalarListSet(values: Vector[Any])
