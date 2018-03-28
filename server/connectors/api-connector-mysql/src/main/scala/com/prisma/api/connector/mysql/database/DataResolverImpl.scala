package com.prisma.api.connector.mysql.database

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.api.connector._
import com.prisma.api.connector.mysql.Metrics
import com.prisma.gc_values._
import com.prisma.shared.models._
import slick.dbio.Effect.Read
import slick.dbio.NoStream
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery
import slick.sql.SqlAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DataResolverImpl(project: Project, readonlyClientDatabase: MySQLProfile.backend.DatabaseDef) extends DataResolver {

  override def resolveByGlobalId(globalId: IdGCValue): Future[Option[PrismaNode]] = { //todo rewrite this to use normal query?
    if (globalId.value == "viewer-fixed") return Future.successful(Some(PrismaNode(globalId, RootGCValue.empty, Some("Viewer"))))

    val query: SqlAction[Option[String], NoStream, Read] = TableQuery(new ProjectRelayIdTable(_, project.id))
      .filter(_.id === globalId.value)
      .map(_.stableModelIdentifier)
      .take(1)
      .result
      .headOption

    readonlyClientDatabase
      .run(query)
      .flatMap {
        case Some(stableModelIdentifier) =>
          val model = project.schema.getModelByStableIdentifier_!(stableModelIdentifier.trim)
          resolveByUnique(NodeSelector.forGraphQLIdGCValue(model, globalId))

        case _ =>
          Future.successful(None)
      }
  }

  override def resolveByModel(model: Model, args: Option[QueryArguments] = None): Future[ResolverResultNew[PrismaNode]] = {
    val query = DatabaseQueryBuilder.selectAllFromTableNew(project.id, model, args)
    performWithTiming("loadModelRowsForExport", readonlyClientDatabase.run(query))
  }

  override def resolveByUnique(where: NodeSelector): Future[Option[PrismaNode]] =
    batchResolveByUnique(where.model, where.field.name, Vector(where.fieldValue)).map(_.headOption)

  override def countByModel(model: Model, whereFilter: Option[DataItemFilterCollection] = None): Future[Int] = {
    val query = DatabaseQueryBuilder.countAllFromModel(project, model, whereFilter)
    performWithTiming("countByModel", readonlyClientDatabase.run(query))
  }

  override def batchResolveByUnique(model: Model, fieldName: String, values: Vector[GCValue]): Future[Vector[PrismaNode]] = {
    val query = DatabaseQueryBuilder.batchSelectFromModelByUnique(project.id, model, fieldName, values)
    performWithTiming("batchResolveByUnique", readonlyClientDatabase.run(query))
  }

  override def batchResolveScalarList(model: Model, listField: Field, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]] = {
    val query = DatabaseQueryBuilder.selectFromScalarList(project.id, model.name, listField, nodeIds)
    performWithTiming("batchResolveScalarList", readonlyClientDatabase.run(query))
  }

  override def resolveByRelationManyModels(fromField: Field,
                                           fromNodeIds: Vector[IdGCValue],
                                           args: Option[QueryArguments]): Future[Vector[ResolverResultNew[PrismaNodeWithParent]]] = {
    val query = DatabaseQueryBuilder.batchSelectAllFromRelatedModel(project, fromField, fromNodeIds, args)
    performWithTiming("resolveByRelation", readonlyClientDatabase.run(query))
  }

  override def countByRelationManyModels(fromField: Field, fromNodeIds: Vector[IdGCValue], args: Option[QueryArguments]): Future[Vector[(IdGCValue, Int)]] = {
    val query = DatabaseQueryBuilder.countAllFromRelatedModels(project, fromField, fromNodeIds, args)
    performWithTiming("countByRelation", readonlyClientDatabase.run(query))
  }

  override def loadListRowsForExport(model: Model, field: Field, args: Option[QueryArguments] = None): Future[ResolverResultNew[ScalarListValues]] = {
    val query = DatabaseQueryBuilder.selectAllFromListTable(project.id, model, field, args, None)
    performWithTiming("loadListRowsForExport", readonlyClientDatabase.run(query))
  }

  override def loadRelationRowsForExport(relationId: String, args: Option[QueryArguments] = None): Future[ResolverResultNew[RelationNode]] = {
    val query = DatabaseQueryBuilder.selectAllFromRelationTable(project.id, relationId, args)
    performWithTiming("loadRelationRowsForExport", readonlyClientDatabase.run(query))
  }

  protected def performWithTiming[A](name: String, f: => Future[A]): Future[A] = Metrics.sqlQueryTimer.timeFuture(project.id, name) { f }
}
