package com.prisma.api.connector

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.gc_values.{GCValue, IdGCValue}
import com.prisma.shared.models.{Field, Model, Project}

import scala.concurrent.Future

trait DataResolver {
  def project: Project

  def resolveByGlobalId(globalId: IdGCValue): Future[Option[PrismaNode]]

  def resolveByModel(model: Model, args: Option[QueryArguments] = None): Future[ResolverResult[PrismaNode]]

  def resolveByUnique(where: NodeSelector): Future[Option[PrismaNode]]

  def countByTable(table: String, whereFilter: Option[DataItemFilterCollection] = None): Future[Int]

  def countByModel(model: Model, whereFilter: Option[DataItemFilterCollection] = None): Future[Int] = countByTable(model.name, whereFilter)

  def batchResolveByUnique(model: Model, field: Field, values: Vector[GCValue]): Future[Vector[PrismaNode]]

  def batchResolveScalarList(model: Model, listField: Field, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]]

  def resolveByRelationManyModels(fromField: Field,
                                  fromNodeIds: Vector[IdGCValue],
                                  args: Option[QueryArguments]): Future[Vector[ResolverResult[PrismaNodeWithParent]]]

  def countByRelationManyModels(fromField: Field, fromNodeIds: Vector[IdGCValue], args: Option[QueryArguments]): Future[Vector[(IdGCValue, Int)]]

  def loadListRowsForExport(model: Model, listField: Field, args: Option[QueryArguments] = None): Future[ResolverResult[ScalarListValues]]

  def loadRelationRowsForExport(relationTableName: String, args: Option[QueryArguments] = None): Future[ResolverResult[RelationNode]]

}
