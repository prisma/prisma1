package com.prisma.api.connector

import com.prisma.gc_values.{StringIdGCValue, IdGCValue}
import com.prisma.shared.models._

import scala.concurrent.Future

trait DataResolver {
  def project: Project

  def getModelForGlobalId(globalId: StringIdGCValue): Future[Option[Model]]

  def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields): Future[Option[PrismaNode]]

  def getNodes(model: Model, queryArguments: QueryArguments, selectedFields: SelectedFields): Future[ResolverResult[PrismaNode]]

  def getRelatedNodes(
      fromField: RelationField,
      fromNodeIds: Vector[IdGCValue],
      queryArguments: QueryArguments,
      selectedFields: SelectedFields
  ): Future[Vector[ResolverResult[PrismaNodeWithParent]]]

  def getScalarListValues(model: Model, listField: ScalarField, queryArguments: QueryArguments): Future[ResolverResult[ScalarListValues]]

  def getScalarListValuesByNodeIds(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]]

  def getRelationNodes(relationTableName: String, queryArguments: QueryArguments): Future[ResolverResult[RelationNode]]

  def countByTable(table: String): Future[Int]

  def countByModel(model: Model, queryArguments: QueryArguments): Future[Int]
}
