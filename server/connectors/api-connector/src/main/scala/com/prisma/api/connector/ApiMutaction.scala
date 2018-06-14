package com.prisma.api.connector

import com.prisma.gc_values.ListGCValue
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models._

sealed trait ApiMutaction

// DATABASE MUTACTIONS
sealed trait DatabaseMutaction extends ApiMutaction {
  def project: Project
}

sealed trait FurtherNestedMutaction extends DatabaseMutaction

// TOP LEVEL - SINGLE
case class CreateDataItem(
    project: Project,
    model: Model,
    nonListArgs: PrismaArgs,
    listArgs: Vector[(String, ListGCValue)],
    nestedCreates: Vector[NestedCreateDataItem],
    nestedConnects: Vector[NestedConnectRelation]
) extends FurtherNestedMutaction

case class UpdateDataItem(
    project: Project,
    where: NodeSelector,
    nonListArgs: PrismaArgs,
    listArgs: Vector[(String, ListGCValue)],
    previousValues: PrismaNode,
    nestedCreates: Vector[NestedCreateDataItem],
    nestedUpdates: Vector[NestedUpdateDataItem],
    nestedUpserts: Vector[NestedUpsertDataItem],
    nestedDeletes: Vector[NestedDeleteDataItem],
    nestedConnects: Vector[NestedConnectRelation],
    nestedDisconnects: Vector[NestedDisconnectRelation]
) extends FurtherNestedMutaction

case class UpsertDataItem(
    project: Project,
    createPath: Path,
    updatePath: Path,
    nonListCreateArgs: PrismaArgs,
    listCreateArgs: Vector[(String, ListGCValue)],
    nonListUpdateArgs: PrismaArgs,
    listUpdateArgs: Vector[(String, ListGCValue)],
    createMutactions: Vector[DatabaseMutaction],
    updateMutactions: Vector[DatabaseMutaction]
) extends FurtherNestedMutaction
case class DeleteDataItem(project: Project, where: NodeSelector, previousValues: PrismaNode) extends DatabaseMutaction

// TOP LEVEL - MANY
case class DeleteDataItems(project: Project, model: Model, whereFilter: Option[Filter]) extends DatabaseMutaction
case class ResetDataMutaction(project: Project, tableNames: Vector[String])             extends DatabaseMutaction
case class UpdateDataItems(project: Project, model: Model, whereFilter: Option[Filter], updateArgs: PrismaArgs, listArgs: Vector[(String, ListGCValue)])
    extends DatabaseMutaction

// NESTED
sealed trait NestedDatabaseMutaction extends DatabaseMutaction {
  def relationField: RelationField
}

case class NestedCreateDataItem(
    project: Project,
    relationField: RelationField,
    nonListArgs: PrismaArgs,
    listArgs: Vector[(String, ListGCValue)],
    nestedCreates: Vector[NestedCreateDataItem],
    nestedConnects: Vector[NestedConnectRelation]
) extends NestedDatabaseMutaction
    with FurtherNestedMutaction

case class NestedUpdateDataItem(
    project: Project,
    relationField: RelationField,
    path: Path,
    nonListArgs: PrismaArgs,
    listArgs: Vector[(String, ListGCValue)],
    nestedCreates: Vector[NestedCreateDataItem],
    nestedUpdates: Vector[NestedUpdateDataItem],
    nestedUpserts: Vector[NestedUpsertDataItem],
    nestedDeletes: Vector[NestedDeleteDataItem],
    nestedConnects: Vector[NestedConnectRelation],
    nestedDisconnects: Vector[NestedDisconnectRelation]
) extends NestedDatabaseMutaction
    with FurtherNestedMutaction

case class NestedUpsertDataItem(
    project: Project,
    relationField: RelationField,
    createPath: Path,
    updatePath: Path,
    createListArgs: Vector[(String, ListGCValue)],
    createNonListArgs: PrismaArgs,
    updateListArgs: Vector[(String, ListGCValue)],
    updateNonListArgs: PrismaArgs,
    createMutactions: Vector[DatabaseMutaction],
    updateMutactions: Vector[DatabaseMutaction]
) extends NestedDatabaseMutaction
    with FurtherNestedMutaction

case class NestedDeleteDataItem(project: Project, relationField: RelationField, path: Path)                                   extends NestedDatabaseMutaction
case class NestedConnectRelation(project: Project, relationField: RelationField, path: Path, topIsCreate: Boolean)            extends NestedDatabaseMutaction
case class NestedDisconnectRelation(project: Project, relationField: RelationField, path: Path, topIsCreate: Boolean = false) extends NestedDatabaseMutaction

// IMPORT
case class PushScalarListsImport(project: Project, tableName: String, id: String, args: ListGCValue)      extends DatabaseMutaction
case class CreateRelationRowsImport(project: Project, relation: Relation, args: Vector[(String, String)]) extends DatabaseMutaction
case class CreateDataItemsImport(project: Project, model: Model, args: Vector[PrismaArgs])                extends DatabaseMutaction

// OBSOLETE ??
case class AddDataItemToManyRelationByPath(project: Project, path: Path)                         extends DatabaseMutaction
case class VerifyConnection(project: Project, path: Path)                                        extends DatabaseMutaction
case class VerifyWhere(project: Project, where: NodeSelector)                                    extends DatabaseMutaction
case class CascadingDeleteRelationMutactions(project: Project, path: Path)                       extends DatabaseMutaction
case class DeleteManyRelationChecks(project: Project, model: Model, whereFilter: Option[Filter]) extends DatabaseMutaction
case class DeleteRelationCheck(project: Project, path: Path)                                     extends DatabaseMutaction

// SIDE EFFECT MUTACTIONS
sealed trait SideEffectMutaction                                                                     extends ApiMutaction
case class PublishSubscriptionEvent(project: Project, value: Map[String, Any], mutationName: String) extends SideEffectMutaction
case class ServerSideSubscription(
    project: Project,
    model: Model,
    mutationType: ModelMutationType,
    function: ServerSideSubscriptionFunction,
    nodeId: Id, //todo
    requestId: String,
    updatedFields: Option[List[String]] = None,
    previousValues: Option[PrismaNode] = None
) extends SideEffectMutaction
