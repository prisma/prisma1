package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.Metrics
import com.prisma.api.connector.jdbc.database.{JdbcApiDatabaseQueryBuilder, SlickDatabase}
import com.prisma.gc_values._
import com.prisma.shared.models._

import scala.concurrent.{ExecutionContext, Future}

case class JdbcDataResolver(
    project: Project,
    slickDatabase: SlickDatabase,
    schemaName: Option[String]
)(implicit ec: ExecutionContext)
    extends DataResolver {

  val queryBuilder = JdbcApiDatabaseQueryBuilder(
    schemaName = schemaName.getOrElse(project.id),
    slickDatabase = slickDatabase
  )(ec)

  override def resolveByGlobalId(globalId: CuidGCValue): Future[Option[PrismaNode]] = { //todo rewrite this to use normal query?
    if (globalId.value == "viewer-fixed") return Future.successful(Some(PrismaNode(globalId, RootGCValue.empty, Some("Viewer"))))

    val query = queryBuilder.selectByGlobalId(project.schema, globalId)
    performWithTiming("resolveByGlobalId", slickDatabase.database.run(query))
  }

  override def resolveByModel(model: Model, args: Option[QueryArguments] = None): Future[ResolverResult[PrismaNode]] = {
    val query = queryBuilder.selectAllFromTable(model, args)
    performWithTiming("loadModelRowsForExport", slickDatabase.database.run(query))
  }

  override def resolveByUnique(where: NodeSelector): Future[Option[PrismaNode]] =
    batchResolveByUnique(where.model, where.field, Vector(where.fieldGCValue)).map(_.headOption)

  override def countByTable(table: String, whereFilter: Option[Filter] = None): Future[Int] = {
    val actualTable = project.schema.getModelByName(table) match {
      case Some(model) => model.dbName
      case None        => table
    }
    val query = queryBuilder.countAllFromTable(actualTable, whereFilter)
    performWithTiming("countByModel", slickDatabase.database.run(query))
  }

  override def batchResolveByUnique(model: Model, field: ScalarField, values: Vector[GCValue]): Future[Vector[PrismaNode]] = {
    val query = queryBuilder.batchSelectFromModelByUnique(model, field, values)
    performWithTiming("batchResolveByUnique", slickDatabase.database.run(query))
  }

  override def batchResolveScalarList(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]] = {
    val query = queryBuilder.selectFromScalarList(model.dbName, listField, nodeIds)
    performWithTiming("batchResolveScalarList", slickDatabase.database.run(query))
  }

  override def resolveByRelationManyModels(
      fromField: RelationField,
      fromNodeIds: Vector[IdGCValue],
      args: Option[QueryArguments]
  ): Future[Vector[ResolverResult[PrismaNodeWithParent]]] = {
    val query = queryBuilder.batchSelectAllFromRelatedModel(project.schema, fromField, fromNodeIds, args)
    performWithTiming("resolveByRelation", slickDatabase.database.run(query))
  }

  override def loadListRowsForExport(model: Model, field: ScalarField, args: Option[QueryArguments] = None): Future[ResolverResult[ScalarListValues]] = {
    val query = queryBuilder.selectAllFromListTable(model, field, args)
    performWithTiming("loadListRowsForExport", slickDatabase.database.run(query))
  }

  override def loadRelationRowsForExport(relationId: String, args: Option[QueryArguments] = None): Future[ResolverResult[RelationNode]] = {
    val relation = project.schema.relations.find(_.relationTableName == relationId).get
    val query    = queryBuilder.selectAllFromRelationTable(relation, args)
    performWithTiming("loadRelationRowsForExport", slickDatabase.database.run(query))
  }

  protected def performWithTiming[A](name: String, f: => Future[A]): Future[A] = Metrics.sqlQueryTimer.timeFuture(project.id, name) { f }

}
