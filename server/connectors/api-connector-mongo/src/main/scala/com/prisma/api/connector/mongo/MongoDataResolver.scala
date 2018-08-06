package com.prisma.api.connector.mongo

import com.prisma.api.connector._
import com.prisma.gc_values.{CuidGCValue, IdGCValue}
import com.prisma.shared.models.{Model, Project, RelationField, ScalarField}

import scala.concurrent.Future

case class MongoDataResolver(project: Project) extends DataResolver {
  override def getModelForGlobalId(globalId: CuidGCValue): Future[Option[Model]] = ???

  override def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields): Future[Option[PrismaNode]] = ???

  override def getNodes(model: Model, args: Option[QueryArguments], selectedFields: SelectedFields): Future[ResolverResult[PrismaNode]] = ???

  override def getRelatedNodes(fromField: RelationField,
                               fromNodeIds: Vector[IdGCValue],
                               args: Option[QueryArguments],
                               selectedFields: SelectedFields): Future[Vector[ResolverResult[PrismaNodeWithParent]]] = ???

  override def getScalarListValues(model: Model, listField: ScalarField, args: Option[QueryArguments]): Future[ResolverResult[ScalarListValues]] = ???

  override def getScalarListValuesByNodeIds(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]] = ???

  override def getRelationNodes(relationTableName: String, args: Option[QueryArguments]): Future[ResolverResult[RelationNode]] = ???

  override def countByTable(table: String, whereFilter: Option[Filter]): Future[Int] = ???
}
