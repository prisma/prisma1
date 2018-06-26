package com.prisma.api.mutactions

import com.prisma.api.connector._
import com.prisma.api.schema.APIErrors.RelationIsRequired
import com.prisma.shared.models.{Model, Project, RelationField}
import com.prisma.util.coolArgs._

case class DatabaseMutactions(project: Project) {

  case class NestedMutactions(
      nestedCreates: Vector[NestedCreateDataItem],
      nestedUpdates: Vector[NestedUpdateDataItem],
      nestedUpserts: Vector[NestedUpsertDataItem],
      nestedDeletes: Vector[NestedDeleteDataItem],
      nestedConnects: Vector[NestedConnectRelation],
      nestedDisconnects: Vector[NestedDisconnectRelation]
  ) {
    def ++(other: NestedMutactions) = NestedMutactions(
      nestedCreates = nestedCreates ++ other.nestedCreates,
      nestedUpdates = nestedUpdates ++ other.nestedUpdates,
      nestedUpserts = nestedUpserts ++ other.nestedUpserts,
      nestedDeletes = nestedDeletes ++ other.nestedDeletes,
      nestedConnects = nestedConnects ++ other.nestedConnects,
      nestedDisconnects = nestedDisconnects ++ other.nestedDisconnects
    )

    val isEmpty    = nestedCreates.isEmpty && nestedUpdates.isEmpty && nestedUpserts.isEmpty && nestedDeletes.isEmpty && nestedConnects.isEmpty && nestedDisconnects.isEmpty
    val isNonEmpty = !isEmpty
  }

  object NestedMutactions {
    val empty = NestedMutactions(Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty)
  }
//
//  def report[T](mutactions: Vector[T]): Vector[T] = {
//    ApiMetrics.mutactionCount.incBy(mutactions.size, project.id)
//    mutactions
//  }

  def getMutactionsForDelete(where: NodeSelector, previousValues: PrismaNode): DeleteDataItem = {
    DeleteDataItem(project = project, where = where, previousValues = previousValues)
  }

  //todo this does not support cascading delete behavior at the moment
  def getMutactionsForDeleteMany(model: Model, whereFilter: Option[Filter]): DeleteDataItems = {
//    val requiredRelationChecks = DeleteManyRelationChecks(project, model, whereFilter)
//    val deleteItems            = DeleteDataItems(project, model, whereFilter)
//    Vector(requiredRelationChecks, deleteItems)
    DeleteDataItems(project, model, whereFilter)
  }

  def getMutactionsForUpdate(model: Model, where: NodeSelector, args: CoolArgs, previousValues: PrismaNode): UpdateDataItem = {
    val (nonListArgs, listArgs) = args.getUpdateArgs(model)
//    val updateMutaction         = UpdateDataItem(project, path, nonListArgs, listArgs, previousValues)
//    val whereFieldValue = args.raw.get(where.field.name)
//    val updatedWhere    = whereFieldValue.map(updateNodeSelectorValue(where)).getOrElse(where)
//    val updatedPath     = path.copy(root = updatedWhere)
    val startingWhere = NodeSelector.forIdGCValue(model, NodeIds.createNodeIdForModel(model))

    val nested = getMutactionsForNestedMutation(args, Some(startingWhere), None, triggeredFromCreate = false)

//    updateMutaction +: nested

    UpdateDataItem(
      project = project,
      where = where,
      nonListArgs = nonListArgs,
      listArgs = listArgs,
      previousValues = previousValues,
      nestedCreates = nested.nestedCreates,
      nestedUpdates = nested.nestedUpdates,
      nestedUpserts = nested.nestedUpserts,
      nestedDeletes = nested.nestedDeletes,
      nestedConnects = nested.nestedConnects,
      nestedDisconnects = nested.nestedDisconnects
    )
  }

  def getMutactionsForUpdateMany(model: Model, whereFilter: Option[Filter], args: CoolArgs): UpdateDataItems = {
    val (nonListArgs, listArgs) = args.getUpdateArgs(model)
    UpdateDataItems(project, model, whereFilter, nonListArgs, listArgs)
  }

  def getMutactionsForCreate(model: Model, args: CoolArgs): CreateDataItem = {
    val (nonListArgs, listArgs) = args.getCreateArgs(model)

    val startingWhere    = NodeSelector.forIdGCValue(model, NodeIds.createNodeIdForModel(model))
    val nestedMutactions = getMutactionsForNestedMutation(args, Some(startingWhere), None, triggeredFromCreate = true)
    CreateDataItem(
      project = project,
      model = model,
      nonListArgs = nonListArgs,
      listArgs = listArgs,
      nestedCreates = nestedMutactions.nestedCreates,
      nestedConnects = nestedMutactions.nestedConnects
    )
  }

  def getMutactionsForUpsert(where: NodeSelector, allArgs: CoolArgs): UpsertDataItem = {
//    val (nonListCreateArgs, listCreateArgs) = allArgs.createArgumentsAsCoolArgs.getCreateArgs(where.model)
//    val (nonListUpdateArgs, listUpdateArgs) = allArgs.updateArgumentsAsCoolArgs.getUpdateArgs(where.model)

//    val createdNestedActions = getNestedMutactionsForUpsert(allArgs.createArgumentsAsCoolArgs, createPath, true)
//    val updateNestedActions  = getNestedMutactionsForUpsert(allArgs.updateArgumentsAsCoolArgs, updatePath, false)
//    val model = field.relatedModel_!
//    val where = upsert match {
//      case x: UpsertByWhere    => Some(x.where)
//      case _: UpsertByRelation => None
//    }
//    val create: NestedCreateDataItem = getMutactionForNestedCreate(model, path, field, triggeredFromCreate = false, upsert.create)
//    val update: NestedUpdateDataItem = getMutactionForNestedUpdate(model, path, field, where, upsert.update)
//    NestedUpsertDataItem(project, field, where, create, update)
    val create = getMutactionsForCreate(where.model, allArgs.createArgumentsAsCoolArgs)
    val update = getMutactionsForUpdate(where.model, where, allArgs.updateArgumentsAsCoolArgs, PrismaNode.dummy)

//    UpsertDataItem(project, where, nonListCreateArgs, listCreateArgs, nonListUpdateArgs, listUpdateArgs, Vector.empty, Vector.empty)

    UpsertDataItem(
      project = project,
      where = where,
      create = create,
      update = update
    )
  }

//  def getNestedMutactionsForUpsert(args: CoolArgs, path: Path, triggeredFromCreate: Boolean): Vector[DatabaseMutaction] = {
//    val x = for {
//      field           <- path.relationFieldsNotOnPathOnLastModel
//      subModel        = field.relatedModel_!
//      nestedMutations = args.subNestedMutation(field, subModel)
//    } yield {
//
//      val checkMutactions                 = getMutactionsForWhereChecks(nestedMutations) ++ getMutactionsForConnectionChecks(subModel, nestedMutations, path, field)
//      val mutactionsThatACreateCanTrigger = getMutactionsForNestedConnectMutation(nestedMutations, path, field, triggeredFromCreate)
//      val otherMutactions = getMutactionsForNestedDisconnectMutation(nestedMutations, path, field) ++ getMutactionsForNestedDeleteMutation(nestedMutations,
//                                                                                                                                           path,
//                                                                                                                                           field)
//      if (triggeredFromCreate && mutactionsThatACreateCanTrigger.isEmpty && field.isRequired) throw RelationIsRequired(field.name, path.lastModel.name)
//
//      checkMutactions ++ mutactionsThatACreateCanTrigger ++ otherMutactions
//    }
//    x.flatten.toVector
//  }

  def getMutactionsForNestedMutation(args: CoolArgs,
                                     startingWhere: Option[NodeSelector],
                                     parentField: Option[RelationField],
                                     triggeredFromCreate: Boolean): NestedMutactions = {

    val model: Model = startingWhere.map(_.model).getOrElse(parentField.get.relatedModel_!)

    val x = for {
      field          <- model.relationFields.filter(field => !parentField.map(_.relatedField).contains(field))
      subModel       = field.relatedModel_!
      nestedMutation = args.subNestedMutation(field, subModel)
    } yield {

      val nestedCreates     = getMutactionsForNestedCreateMutation(subModel, nestedMutation, field, triggeredFromCreate)
      val nestedUpdates     = getMutactionsForNestedUpdateMutation(nestedMutation, field)
      val nestedUpserts     = getMutactionsForNestedUpsertMutation(nestedMutation, field)
      val nestedDeletes     = getMutactionsForNestedDeleteMutation(nestedMutation, field)
      val nestedConnects    = getMutactionsForNestedConnectMutation(nestedMutation, field, triggeredFromCreate)
      val nestedDisconnects = getMutactionsForNestedDisconnectMutation(nestedMutation, field)

      val mutactionsThatACreateCanTrigger = nestedCreates ++ nestedConnects

      if (triggeredFromCreate && mutactionsThatACreateCanTrigger.isEmpty && field.isRequired) throw RelationIsRequired(field.name, model.name)

      NestedMutactions(
        nestedCreates = nestedCreates,
        nestedUpdates = nestedUpdates,
        nestedUpserts = nestedUpserts,
        nestedDeletes = nestedDeletes,
        nestedConnects = nestedConnects,
        nestedDisconnects = nestedDisconnects
      )
    }
    x.foldLeft(NestedMutactions.empty)(_ ++ _)
  }

  def getMutactionsForNestedCreateMutation(
      model: Model,
      nestedMutation: NestedMutations,
      field: RelationField,
      triggeredFromCreate: Boolean
  ): Vector[NestedCreateDataItem] = {
    nestedMutation.creates.map { create =>
      val (nonListArgs, listArgs) = create.data.getCreateArgs(model)
      val nestedMutactions        = getMutactionsForNestedMutation(create.data, None, Some(field), triggeredFromCreate = true)
      NestedCreateDataItem(
        project = project,
        relationField = field,
        nonListArgs = nonListArgs,
        listArgs = listArgs,
        nestedCreates = nestedMutactions.nestedCreates,
        nestedConnects = nestedMutactions.nestedConnects,
        topIsCreate = triggeredFromCreate
      )
    }
  }

  def getMutactionForNestedCreate(
      model: Model,
      field: RelationField,
      triggeredFromCreate: Boolean,
      create: CoolArgs
  ): NestedCreateDataItem = {
    val (nonListArgs, listArgs) = create.getCreateArgs(model)
    val nestedMutactions        = getMutactionsForNestedMutation(create, None, Some(field), triggeredFromCreate = true)
    NestedCreateDataItem(
      project = project,
      relationField = field,
      nonListArgs = nonListArgs,
      listArgs = listArgs,
      nestedCreates = nestedMutactions.nestedCreates,
      nestedConnects = nestedMutactions.nestedConnects,
      topIsCreate = triggeredFromCreate
    )
  }

  def getMutactionForNestedUpdate(
      model: Model,
      field: RelationField,
      where: Option[NodeSelector],
      args: CoolArgs
  ): NestedUpdateDataItem = {
    val (nonListArgs, listArgs) = args.getUpdateArgs(model)
    val nestedMutactions        = getMutactionsForNestedMutation(args, None, Some(field), triggeredFromCreate = false)
    NestedUpdateDataItem(
      project = project,
      relationField = field,
      where = where,
      nonListArgs = nonListArgs,
      listArgs = listArgs,
      nestedCreates = nestedMutactions.nestedCreates,
      nestedConnects = nestedMutactions.nestedConnects,
      nestedUpdates = nestedMutactions.nestedUpdates,
      nestedUpserts = nestedMutactions.nestedUpserts,
      nestedDeletes = nestedMutactions.nestedDeletes,
      nestedDisconnects = nestedMutactions.nestedDisconnects
    )
  }

  def getMutactionsForNestedConnectMutation(nestedMutation: NestedMutations, field: RelationField, topIsCreate: Boolean): Vector[NestedConnectRelation] = {
    nestedMutation.connects.map(connect => NestedConnectRelation(project, field, connect.where, topIsCreate))
  }

  def getMutactionsForNestedDisconnectMutation(nestedMutation: NestedMutations, field: RelationField): Vector[NestedDisconnectRelation] = {
    nestedMutation.disconnects.map {
      case _: DisconnectByRelation       => NestedDisconnectRelation(project, field, where = None)
      case disconnect: DisconnectByWhere => NestedDisconnectRelation(project, field, where = Some(disconnect.where))
    }
  }

  def getMutactionsForNestedDeleteMutation(nestedMutation: NestedMutations, field: RelationField): Vector[NestedDeleteDataItem] = {
    nestedMutation.deletes.map { delete =>
      val childWhere = delete match {
        case DeleteByRelation(_)  => None
        case DeleteByWhere(where) => Some(where)

      }
      NestedDeleteDataItem(project, field, childWhere)
    }
  }

  def getMutactionsForNestedUpdateMutation(nestedMutation: NestedMutations, field: RelationField): Vector[NestedUpdateDataItem] = {
    nestedMutation.updates.map { update =>
      val (nonListArgs, listArgs) = update.data.getUpdateArgs(field.model)
      val nested                  = getMutactionsForNestedMutation(update.data, None, Some(field), triggeredFromCreate = false)

      val where = update match {
        case x: UpdateByWhere    => Some(x.where)
        case _: UpdateByRelation => None
      }

      NestedUpdateDataItem(
        project = project,
        relationField = field,
        where = where,
        nonListArgs = nonListArgs,
        listArgs = listArgs,
        nestedCreates = nested.nestedCreates,
        nestedUpdates = nested.nestedUpdates,
        nestedUpserts = nested.nestedUpserts,
        nestedDeletes = nested.nestedDeletes,
        nestedConnects = nested.nestedConnects,
        nestedDisconnects = nested.nestedDisconnects
      )
    }
  }

  def getMutactionsForNestedUpsertMutation(nestedMutation: NestedMutations, field: RelationField): Vector[NestedUpsertDataItem] = {
    nestedMutation.upserts.map { upsert =>
//      val extendedPath = extend(path, field, upsert)
//      val createWhere  = NodeSelector.forIdGCValue(extendedPath.lastModel, NodeIds.createNodeIdForModel(extendedPath.lastModel))

//      val pathForUpdate = upsert match {
//        case upsert: UpsertByWhere => extendedPath.lastEdgeToNodeEdge(upsert.where)
//        case _: UpsertByRelation   => extendedPath
//      }
//      val pathForCreate = extendedPath.lastEdgeToNodeEdge(createWhere)
//
//      val (nonListCreateArgs, listCreateArgs) = upsert.create.getCreateArgs(pathForCreate)
//      val (nonListUpdateArgs, listUpdateArgs) = upsert.update.getUpdateArgs(pathForUpdate.lastModel)

//      val createdNestedActions = getNestedMutactionsForUpsert(upsert.create, pathForCreate, triggeredFromCreate = true)
//      val updateNestedActions  = getNestedMutactionsForUpsert(upsert.update, pathForUpdate, triggeredFromCreate = false)

//      Vector(
      //        UpsertDataItemIfInRelationWith(
      //          project = project,
      //          createPath = pathForCreate,
      //          updatePath = pathForUpdate,
      //          createListArgs = listCreateArgs,
      //          createNonListArgs = nonListCreateArgs,
      //          updateListArgs = listUpdateArgs,
      //          updateNonListArgs = nonListUpdateArgs,
      //          createdNestedActions,
      //          updateNestedActions
      //        ))
      val model = field.relatedModel_!
      val where = upsert match {
        case x: UpsertByWhere    => Some(x.where)
        case _: UpsertByRelation => None
      }
      val create: NestedCreateDataItem = getMutactionForNestedCreate(model, field, triggeredFromCreate = false, upsert.create)
      val update: NestedUpdateDataItem = getMutactionForNestedUpdate(model, field, where, upsert.update)
      NestedUpsertDataItem(project, field, where, create, update)
    }
  }

  private def currentWhere(where: NodeSelector, args: CoolArgs) = {
    val whereFieldValue = args.raw.get(where.field.name)
    val updatedWhere    = whereFieldValue.map(updateNodeSelectorValue(where)).getOrElse(where)
    updatedWhere
  }

//  def generateCascadingDeleteMutactions(startPoint: Path): Vector[DatabaseMutaction] = {
//    def getMutactionsForEdges(paths: Vector[Path]): Vector[DatabaseMutaction] = {
//      paths.filter(_.edges.length > startPoint.edges.length) match {
//        case x if x.isEmpty =>
//          Vector.empty
//
//        case pathsList =>
//          val maxPathLength     = pathsList.map(_.edges.length).max
//          val longestPaths      = pathsList.filter(_.edges.length == maxPathLength)
//          val longestMutactions = longestPaths.map(CascadingDeleteRelationMutactions(project, _))
//          val shortenedPaths    = longestPaths.map(_.removeLastEdge)
//          val newPaths          = pathsList.filter(_.edges.length < maxPathLength) ++ shortenedPaths
//
//          longestMutactions ++ getMutactionsForEdges(newPaths)
//      }
//    }
//
//    val paths: Vector[Path] = Path.collectCascadingPaths(startPoint)
//    getMutactionsForEdges(paths)
//  }

  def extend(path: Path, field: RelationField, nestedMutation: NestedMutation): Path = {
    nestedMutation match {
      case x: NestedWhere => path.append(NodeEdge(field, x.where))
      case _              => path.append(ModelEdge(field))
    }
  }

  def extend(path: Path, field: RelationField): Path = path.append(ModelEdge(field))

  def updateNodeSelectorValue(nodeSelector: NodeSelector)(value: Any): NodeSelector = {
    val unwrapped = value match {
      case Some(x) => x
      case x       => x
    }

    val newGCValue = GCAnyConverter(nodeSelector.field.typeIdentifier, isList = false).toGCValue(unwrapped).get
    nodeSelector.copy(fieldGCValue = newGCValue)
  }
}
