package com.prisma.api.connector.postgresql.database

import java.sql.ResultSet

import com.prisma.api.connector._
import com.prisma.gc_values._
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Function => _, _}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc._

import scala.concurrent.ExecutionContext

case class PostgresApiDatabaseQueryBuilder(
    project: Project,
    schemaName: String
)(implicit ec: ExecutionContext) {
  import JdbcExtensions._
  import PostgresSlickExtensions._
  import com.prisma.slick.NewJdbcExtensions._
  import JooqQueryBuilders._

  private def readsScalarListField(field: ScalarField): ReadsResultSet[ScalarListElement] = ReadsResultSet { rs =>
    val nodeId   = rs.getString(nodeIdFieldName)
    val position = rs.getInt(positionFieldName)
    val value    = rs.getGcValue(valueFieldName, field.typeIdentifier)
    ScalarListElement(nodeId, position, value)
  }

  private def readsPrismaNode(model: Model): ReadsResultSet[PrismaNode] = ReadsResultSet { rs =>
    readPrismaNode(model, rs)
  }

  private def readPrismaNodeWithParent(model: Model, side: RelationSide.Value, oppositeSide: RelationSide.Value) = ReadsResultSet { rs =>
    val node       = readPrismaNode(model, rs)
    val firstSide  = rs.getParentId(side)
    val secondSide = rs.getParentId(oppositeSide)
    val parentId   = if (firstSide == node.id) secondSide else firstSide

    PrismaNodeWithParent(parentId, node)
  }

  private def readPrismaNode(model: Model, rs: ResultSet) = {
    val data = model.scalarNonListFields.map(field => field.name -> rs.getGcValue(field.dbName, field.typeIdentifier))
    PrismaNode(id = rs.getId(model), data = RootGCValue(data: _*), Some(model.name))
  }

  private def readRelation(relation: Relation): ReadsResultSet[RelationNode] = ReadsResultSet { resultSet =>
    val modelAColumn = relation.columnForRelationSide(RelationSide.A)
    val modelBColumn = relation.columnForRelationSide(RelationSide.B)
    RelationNode(a = resultSet.getAsID(modelAColumn), b = resultSet.getAsID(modelBColumn))
  }

  def selectAllFromTable(
      model: Model,
      args: Option[QueryArguments],
      overrideMaxNodeCount: Option[Int] = None
  ): DBIO[ResolverResult[PrismaNode]] = {
    SimpleDBIO[ResolverResult[PrismaNode]] { ctx =>
      val jooqBuilder = JooqModelQueryBuilder(schemaName, model, args)
      val ps          = ctx.connection.prepareStatement(jooqBuilder.queryString)
      JooqSetParams.setQueryArgs(ps, args)
      val rs: ResultSet              = ps.executeQuery()
      val result: Vector[PrismaNode] = rs.as[PrismaNode](readsPrismaNode(model))
      ResolverResult(args, result)
    }
  }

  def batchSelectAllFromRelatedModel(
      schema: Schema,
      fromField: RelationField,
      fromModelIds: Vector[CuidGCValue],
      args: Option[QueryArguments]
  ): DBIO[Vector[ResolverResult[PrismaNodeWithParent]]] = {
    SimpleDBIO[Vector[ResolverResult[PrismaNodeWithParent]]] { ctx =>
      val builder  = JooqRelatedModelsQueryBuilder(schemaName, fromField, args, fromModelIds)
      val query    = if (args.exists(_.isWithPagination)) builder.queryStringWithPagination else builder.queryStringWithoutPagination

      val ps = ctx.connection.prepareStatement(query)

      // injecting params
      val pp     = new PositionedParameters(ps)
      val filter = args.flatMap(_.filter)
      fromModelIds.foreach(pp.setGcValue)
      filter.foreach(filter => JooqSetParams.setParams(pp, filter))

      if (args.get.after.isDefined)       {
        pp.setString(args.get.after.get)
        pp.setString(args.get.after.get)
      }

      if (args.get.before.isDefined) {
        pp.setString(args.get.before.get)
        pp.setString(args.get.before.get)
      }

      if (args.exists(_.isWithPagination)) {
        val params = JooqLimitClauseBuilder.limitClauseForWindowFunction(args)
        pp.setInt(params._1)
        pp.setInt(params._2)
      }

      // executing
      val rs: ResultSet       = ps.executeQuery()
      val result              = rs.as[PrismaNodeWithParent](readPrismaNodeWithParent(fromField.relatedModel_!, fromField.relationSide, fromField.oppositeRelationSide))
      val itemGroupsByModelId = result.groupBy(_.parentId)
      fromModelIds.map { id =>
        itemGroupsByModelId.find(_._1 == id) match {
          case Some((_, itemsForId)) => ResolverResult(args, itemsForId, parentModelId = Some(id))
          case None                  => ResolverResult(Vector.empty[PrismaNodeWithParent], hasPreviousPage = false, hasNextPage = false, parentModelId = Some(id))
        }
      }
    }
  }

  def selectAllFromRelationTable(
      relation: Relation,
      args: Option[QueryArguments]
  ): DBIO[ResolverResult[RelationNode]] = {

    SimpleDBIO[ResolverResult[RelationNode]] { ctx =>
      val builder = JooqRelationQueryBuilder(schemaName, relation, args)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      JooqSetParams.setQueryArgs(ps, args)
      val rs: ResultSet = ps.executeQuery()
      val result        = rs.as(readRelation(relation))
      ResolverResult(result)
    }
  }

  def selectAllFromListTable(
      model: Model,
      field: ScalarField,
      args: Option[QueryArguments]
  ): DBIO[ResolverResult[ScalarListValues]] = {

    SimpleDBIO[ResolverResult[ScalarListValues]] { ctx =>
      val builder = JooqScalarListQueryBuilder(schemaName, field, args)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      JooqSetParams.setQueryArgs(ps, args)
      val rs: ResultSet = ps.executeQuery()

      val result = rs.as(readsScalarListField(field))

      val convertedValues = result
        .groupBy(_.nodeId)
        .map { case (id, values) => ScalarListValues(CuidGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }
        .toVector

      ResolverResult(convertedValues)
    }
  }

  def selectFromScalarList(modelName: String, field: ScalarField, nodeIds: Vector[CuidGCValue]): DBIO[Vector[ScalarListValues]] = {
    SimpleDBIO[Vector[ScalarListValues]] { ctx =>
      val builder = JooqScalarListByUniquesQueryBuilder(schemaName, field, nodeIds)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      val pp      = new PositionedParameters(ps)
      nodeIds.foreach(pp.setGcValue)

      val rs                 = ps.executeQuery()
      val scalarListElements = rs.as(readsScalarListField(field))

      val grouped: Map[Id, Vector[ScalarListElement]] = scalarListElements.groupBy(_.nodeId)
      grouped.map { case (id, values) => ScalarListValues(CuidGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }.toVector
    }
  }

  def countAllFromTable(table: String, whereFilter: Option[Filter]): DBIO[Int] = {
    SimpleDBIO[Int] { ctx =>
      val builder = JooqCountQueryBuilder(schemaName, table, whereFilter)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      JooqSetParams.setFilter(new PositionedParameters(ps), whereFilter)
      val rs = ps.executeQuery()
      rs.next()
      rs.getInt(1)
    }
  }

  def batchSelectFromModelByUnique(model: Model, field: ScalarField, values: Vector[GCValue]): DBIO[Vector[PrismaNode]] = {
    SimpleDBIO { ctx =>
      val queryArgs = Some(QueryArguments.withFilter(ScalarFilter(field, In(values))))
      val builder   = JooqModelQueryBuilder(schemaName, model, queryArgs)
      val ps        = ctx.connection.prepareStatement(builder.queryString)
      JooqSetParams.setQueryArgs(ps, queryArgs)
      val rs: ResultSet = ps.executeQuery()
      rs.as(readsPrismaNode(model))
    }
  }
}
