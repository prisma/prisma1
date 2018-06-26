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

//  def report[T](mutactions: Vector[T]): Vector[T] = {
//    ApiMetrics.mutactionCount.incBy(mutactions.size, project.id)
//    mutactions
//  }

  def getMutactionsForDelete(where: NodeSelector, previousValues: PrismaNode): DeleteDataItem = {
    DeleteDataItem(project = project, where = where, previousValues = previousValues)
  }

  //todo this does not support cascading delete behavior at the moment
  def getMutactionsForDeleteMany(model: Model, whereFilter: Option[Filter]): DeleteDataItems = DeleteDataItems(project, model, whereFilter)

  def getMutactionsForUpdate(model: Model, where: NodeSelector, args: CoolArgs, previousValues: PrismaNode): UpdateDataItem = {
    val (nonListArgs, listArgs)  = args.getUpdateArgs(model)
    val nested: NestedMutactions = getMutactionsForNestedMutation(args, model, None, triggeredFromCreate = false)

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

    val nestedMutactions = getMutactionsForNestedMutation(args, model, None, triggeredFromCreate = true)
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
    val creates = getMutactionsForCreate(where.model, allArgs.createArgumentsAsCoolArgs)
    val updates = getMutactionsForUpdate(where.model, where, allArgs.updateArgumentsAsCoolArgs, PrismaNode.dummy)

    UpsertDataItem(
      project = project,
      where = where,
      create = creates,
      update = updates
    )
  }

  def getMutactionsForNestedMutation(args: CoolArgs,
                                     parentModel: Model,
                                     currentField: Option[RelationField],
                                     triggeredFromCreate: Boolean): NestedMutactions = {

    val x = for {
      parentField    <- parentModel.relationFields.filter(field => !currentField.contains(field))
      subModel       = parentField.relatedModel_!
      nestedMutation = args.subNestedMutation(parentField, subModel)
    } yield {

      val nestedCreates     = getMutactionsForNestedCreateMutation(subModel, nestedMutation, parentField, triggeredFromCreate)
      val nestedUpdates     = getMutactionsForNestedUpdateMutation(subModel, nestedMutation, parentField)
      val nestedUpserts     = getMutactionsForNestedUpsertMutation(nestedMutation, parentField)
      val nestedDeletes     = getMutactionsForNestedDeleteMutation(nestedMutation, parentField)
      val nestedConnects    = getMutactionsForNestedConnectMutation(nestedMutation, parentField, triggeredFromCreate)
      val nestedDisconnects = getMutactionsForNestedDisconnectMutation(nestedMutation, parentField)

      val mutactionsThatACreateCanTrigger = nestedCreates ++ nestedConnects

      if (triggeredFromCreate && mutactionsThatACreateCanTrigger.isEmpty && parentField.isRequired) throw RelationIsRequired(parentField.name, parentModel.name)

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
      parentField: RelationField,
      triggeredFromCreate: Boolean
  ): Vector[NestedCreateDataItem] = {
    nestedMutation.creates.map { create =>
      val (nonListArgs, listArgs) = create.data.getCreateArgs(model)
      val nestedMutactions        = getMutactionsForNestedMutation(create.data, model, Some(parentField.relatedField), triggeredFromCreate = true)
      NestedCreateDataItem(
        project = project,
        parentField = parentField,
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
    val nestedMutactions        = getMutactionsForNestedMutation(create, model, Some(field.relatedField), triggeredFromCreate = true)
    NestedCreateDataItem(
      project = project,
      parentField = field,
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
    val nestedMutactions        = getMutactionsForNestedMutation(args, model, Some(field.relatedField), triggeredFromCreate = false)
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

  def getMutactionsForNestedUpdateMutation(model: Model, nestedMutation: NestedMutations, parentField: RelationField): Vector[NestedUpdateDataItem] = {
    nestedMutation.updates.map { update =>
      val (nonListArgs, listArgs) = update.data.getUpdateArgs(model)
      val nested                  = getMutactionsForNestedMutation(update.data, model, Some(parentField.relatedField), triggeredFromCreate = false)

      val where = update match {
        case x: UpdateByWhere    => Some(x.where)
        case _: UpdateByRelation => None
      }

      NestedUpdateDataItem(
        project = project,
        relationField = parentField,
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

  def getMutactionsForNestedUpsertMutation(nestedMutation: NestedMutations, parentField: RelationField): Vector[NestedUpsertDataItem] = {
    nestedMutation.upserts.map { upsert =>
      val subModel = parentField.relatedModel_!
      val where = upsert match {
        case x: UpsertByWhere    => Some(x.where)
        case _: UpsertByRelation => None
      }
      val create: NestedCreateDataItem = getMutactionForNestedCreate(subModel, parentField, triggeredFromCreate = false, upsert.create)
      val update: NestedUpdateDataItem = getMutactionForNestedUpdate(subModel, parentField, where, upsert.update)
      NestedUpsertDataItem(project, parentField, where, create, update)
    }
  }
}
