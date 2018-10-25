package com.prisma.api.mutactions

import com.prisma.api.connector._
import com.prisma.api.schema.APIErrors.RelationIsRequired
import com.prisma.shared.models.{Model, Project, RelationField}
import com.prisma.util.coolArgs._

case class DatabaseMutactions(project: Project) {

  case class NestedMutactions(
      nestedCreates: Vector[NestedCreateNode],
      nestedUpdates: Vector[NestedUpdateNode],
      nestedUpserts: Vector[NestedUpsertNode],
      nestedDeletes: Vector[NestedDeleteNode],
      nestedConnects: Vector[NestedConnect],
      nestedDisconnects: Vector[NestedDisconnect],
      nestedUpdateManys: Vector[NestedUpdateNodes]
  ) {
    def ++(other: NestedMutactions) = NestedMutactions(
      nestedCreates = nestedCreates ++ other.nestedCreates,
      nestedUpdates = nestedUpdates ++ other.nestedUpdates,
      nestedUpserts = nestedUpserts ++ other.nestedUpserts,
      nestedDeletes = nestedDeletes ++ other.nestedDeletes,
      nestedConnects = nestedConnects ++ other.nestedConnects,
      nestedDisconnects = nestedDisconnects ++ other.nestedDisconnects,
      nestedUpdateManys = nestedUpdateManys ++ other.nestedUpdateManys
    )

    val isEmpty    = nestedCreates.isEmpty && nestedUpdates.isEmpty && nestedUpserts.isEmpty && nestedDeletes.isEmpty && nestedConnects.isEmpty && nestedDisconnects.isEmpty && nestedUpdateManys.isEmpty
    val isNonEmpty = !isEmpty
  }

  object NestedMutactions {
    val empty = NestedMutactions(Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty)
  }

//  def report[T](mutactions: Vector[T]): Vector[T] = {
//    ApiMetrics.mutactionCount.incBy(mutactions.size, project.id)
//    mutactions
//  }

  def getMutactionsForDelete(where: NodeSelector, previousValues: PrismaNode): TopLevelDeleteNode = {
    TopLevelDeleteNode(project = project, where = where, previousValues = previousValues)
  }

  //todo this does not support cascading delete behavior at the moment
  def getMutactionsForDeleteMany(model: Model, whereFilter: Option[Filter]): DeleteNodes = DeleteNodes(project, model, whereFilter)

  def getMutactionsForUpdate(model: Model, where: NodeSelector, args: CoolArgs): TopLevelUpdateNode = {
    val (nonListArgs, listArgs)  = args.getUpdateArgs(model)
    val nested: NestedMutactions = getMutactionsForNestedMutation(args, model, None, triggeredFromCreate = false)

    TopLevelUpdateNode(
      project = project,
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

  def getMutactionsForUpdateMany(model: Model, whereFilter: Option[Filter], args: CoolArgs): UpdateNodes = {
    val (nonListArgs, listArgs) = args.getUpdateArgs(model)
    UpdateNodes(project, model, whereFilter, nonListArgs, listArgs)
  }

  def getMutactionsForCreate(model: Model, args: CoolArgs): TopLevelCreateNode = {
    val (nonListArgs, listArgs) = args.getCreateArgs(model)

    val nestedMutactions = getMutactionsForNestedMutation(args, model, None, triggeredFromCreate = true)
    TopLevelCreateNode(
      project = project,
      model = model,
      nonListArgs = nonListArgs,
      listArgs = listArgs,
      nestedCreates = nestedMutactions.nestedCreates,
      nestedConnects = nestedMutactions.nestedConnects
    )
  }

  def getMutactionsForUpsert(where: NodeSelector, allArgs: CoolArgs): TopLevelUpsertNode = {
    val creates = getMutactionsForCreate(where.model, allArgs.createArgumentsAsCoolArgs)
    val updates = getMutactionsForUpdate(where.model, where, allArgs.updateArgumentsAsCoolArgs)

    TopLevelUpsertNode(
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
      val nestedUpdateManys = getMutactionsForNestedUpdateManyMutation(subModel, nestedMutation, parentField)

      val mutactionsThatACreateCanTrigger = nestedCreates ++ nestedConnects

      if (triggeredFromCreate && mutactionsThatACreateCanTrigger.isEmpty && parentField.isRequired) throw RelationIsRequired(parentField.name, parentModel.name)

      NestedMutactions(
        nestedCreates = nestedCreates,
        nestedUpdates = nestedUpdates,
        nestedUpserts = nestedUpserts,
        nestedDeletes = nestedDeletes,
        nestedConnects = nestedConnects,
        nestedDisconnects = nestedDisconnects,
        nestedUpdateManys = nestedUpdateManys
      )
    }
    x.foldLeft(NestedMutactions.empty)(_ ++ _)
  }

  def getMutactionsForNestedCreateMutation(
      model: Model,
      nestedMutation: NestedMutations,
      parentField: RelationField,
      triggeredFromCreate: Boolean
  ): Vector[NestedCreateNode] = {
    nestedMutation.creates.map { create =>
      val (nonListArgs, listArgs) = create.data.getCreateArgs(model)
      val nestedMutactions        = getMutactionsForNestedMutation(create.data, model, Some(parentField.relatedField), triggeredFromCreate = true)
      NestedCreateNode(
        project = project,
        relationField = parentField,
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
  ): NestedCreateNode = {
    val (nonListArgs, listArgs) = create.getCreateArgs(model)
    val nestedMutactions        = getMutactionsForNestedMutation(create, model, Some(field.relatedField), triggeredFromCreate = true)
    NestedCreateNode(
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
  ): NestedUpdateNode = {
    val (nonListArgs, listArgs) = args.getUpdateArgs(model)
    val nestedMutactions        = getMutactionsForNestedMutation(args, model, Some(field.relatedField), triggeredFromCreate = false)
    NestedUpdateNode(
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

  def getMutactionsForNestedConnectMutation(nestedMutation: NestedMutations, field: RelationField, topIsCreate: Boolean): Vector[NestedConnect] = {
    nestedMutation.connects.map(connect => NestedConnect(project, field, connect.where, topIsCreate))
  }

  def getMutactionsForNestedDisconnectMutation(nestedMutation: NestedMutations, field: RelationField): Vector[NestedDisconnect] = {
    nestedMutation.disconnects.map {
      case _: DisconnectByRelation  => NestedDisconnect(project, field, where = None)
      case DisconnectByWhere(where) => NestedDisconnect(project, field, where = Some(where))
    }
  }

  def getMutactionsForNestedDeleteMutation(nestedMutation: NestedMutations, field: RelationField): Vector[NestedDeleteNode] = {
    nestedMutation.deletes.map {
      case _: DeleteByRelation  => NestedDeleteNode(project, field, where = None)
      case DeleteByWhere(where) => NestedDeleteNode(project, field, where = Some(where))
    }
  }

  def getMutactionsForNestedUpdateMutation(model: Model, nestedMutation: NestedMutations, parentField: RelationField): Vector[NestedUpdateNode] = {
    nestedMutation.updates.map { update =>
      val (nonListArgs, listArgs) = update.data.getUpdateArgs(model)
      val nested                  = getMutactionsForNestedMutation(update.data, model, Some(parentField.relatedField), triggeredFromCreate = false)

      val where = update match {
        case x: UpdateByWhere    => Some(x.where)
        case _: UpdateByRelation => None
      }

      NestedUpdateNode(
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

  def getMutactionsForNestedUpdateManyMutation(model: Model, nestedMutation: NestedMutations, parentField: RelationField): Vector[NestedUpdateNodes] = {
    nestedMutation.updateManys.map { updateMany =>
      val (nonListArgs, listArgs) = updateMany.data.getUpdateArgs(model)

      NestedUpdateNodes(project, model, parentField, updateMany.whereFilter, nonListArgs, listArgs)
    }
  }

  def getMutactionsForNestedUpsertMutation(nestedMutation: NestedMutations, parentField: RelationField): Vector[NestedUpsertNode] = {
    nestedMutation.upserts.map { upsert =>
      val subModel = parentField.relatedModel_!
      val where = upsert match {
        case x: UpsertByWhere    => Some(x.where)
        case _: UpsertByRelation => None
      }
      val create: NestedCreateNode = getMutactionForNestedCreate(subModel, parentField, triggeredFromCreate = false, upsert.create)
      val update: NestedUpdateNode = getMutactionForNestedUpdate(subModel, parentField, where, upsert.update)
      NestedUpsertNode(project, parentField, where, create, update)
    }
  }
}
