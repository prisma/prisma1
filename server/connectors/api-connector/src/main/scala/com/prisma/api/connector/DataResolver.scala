package com.prisma.api.connector

import com.prisma.gc_values.{CuidGCValue, GCValue, IdGcValue}
import com.prisma.shared.models._

import scala.concurrent.Future

trait DataResolver {
  def project: Project

  def resolveByGlobalId(globalId: CuidGCValue): Future[Option[PrismaNode]]

  def resolveByModel(model: Model, args: Option[QueryArguments] = None): Future[ResolverResult[PrismaNode]]

  def resolveByUnique(where: NodeSelector): Future[Option[PrismaNode]]

  def countByTable(table: String, whereFilter: Option[Filter] = None): Future[Int]

  def countByModel(model: Model, whereFilter: Option[Filter] = None): Future[Int] = countByTable(model.name, whereFilter)

  def batchResolveByUnique(model: Model, field: ScalarField, values: Vector[GCValue]): Future[Vector[PrismaNode]]

  def batchResolveScalarList(model: Model, listField: ScalarField, nodeIds: Vector[IdGcValue]): Future[Vector[ScalarListValues]]

  def resolveByRelationManyModels(
      fromField: RelationField,
      fromNodeIds: Vector[IdGcValue],
      args: Option[QueryArguments]
  ): Future[Vector[ResolverResult[PrismaNodeWithParent]]]

  def loadListRowsForExport(model: Model, listField: ScalarField, args: Option[QueryArguments] = None): Future[ResolverResult[ScalarListValues]]

  def loadRelationRowsForExport(relationTableName: String, args: Option[QueryArguments] = None): Future[ResolverResult[RelationNode]]

}
