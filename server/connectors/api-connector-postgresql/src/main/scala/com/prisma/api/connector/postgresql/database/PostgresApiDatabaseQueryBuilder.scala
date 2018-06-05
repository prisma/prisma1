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

  private def readsScalarListField(field: ScalarField): ReadsResultSet[ScalarListElement] = ReadsResultSet { rs =>
    val nodeId   = rs.getString("nodeId")
    val position = rs.getInt("position")
    val value    = rs.getGcValue("value", field.typeIdentifier)
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
    PrismaNode(id = rs.getId(model), data = RootGCValue(data: _*))
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
      // prepare statement
      val builder = ModelQueryBuilder(schemaName, model, args)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      SetParams.setQueryArgs(ps, args)
      // execute
      val rs: ResultSet = ps.executeQuery()
      // read result
      val result: Vector[PrismaNode] = rs.as[PrismaNode](readsPrismaNode(model))
      ResolverResult(args, result)
    }
  }

  def batchSelectAllFromRelatedModel(
      schema: Schema,
      fromField: RelationField,
      fromModelIds: Vector[IdGCValue],
      args: Option[QueryArguments]
  ): DBIO[Vector[ResolverResult[PrismaNodeWithParent]]] = {
    val isWithPagination = args.flatMap(_.last).orElse(args.flatMap(_.first)).isDefined
    if (isWithPagination) {
      batchSelectAllFromRelatedModelWithPagination(schema, fromField, fromModelIds, args)
    } else {
      batchSelectAllFromRelatedModelWithoutPagination(schema, fromField, fromModelIds, args)
    }

  }

  def batchSelectAllFromRelatedModelWithoutPagination(
      schema: Schema,
      fromField: RelationField,
      fromModelIds: Vector[IdGCValue],
      args: Option[QueryArguments]
  ): DBIO[Vector[ResolverResult[PrismaNodeWithParent]]] = {
    SimpleDBIO[Vector[ResolverResult[PrismaNodeWithParent]]] { ctx =>
      val builder = RelatedModelsQueryBuilder(schemaName, fromField, args, fromModelIds)
      val ps      = ctx.connection.prepareStatement(builder.queryStringWithoutPagination)

      // injecting params
      val pp     = new PositionedParameters(ps)
      val filter = args.flatMap(_.filter)
      fromModelIds.distinct.foreach(pp.setGcValue)
      filter.foreach(filter => SetParams.setParams(pp, filter))

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

  def batchSelectAllFromRelatedModelWithPagination(
      schema: Schema,
      fromField: RelationField,
      fromModelIds: Vector[IdGCValue],
      args: Option[QueryArguments]
  ): DBIO[Vector[ResolverResult[PrismaNodeWithParent]]] = {
    SimpleDBIO[Vector[ResolverResult[PrismaNodeWithParent]]] { ctx =>
      val builder = RelatedModelsQueryBuilder(schemaName, fromField, args, fromModelIds)
      // see https://github.com/graphcool/internal-docs/blob/master/relations.md#findings

      val baseQuery = "(" + builder.queryStringWithPagination + ")"

      val distinctModelIds = fromModelIds.distinct

      val queries = Vector.fill(distinctModelIds.size)(baseQuery)
      val query   = queries.mkString(" union all ")

      val ps = ctx.connection.prepareStatement(query)

      // injecting params
      val pp     = new PositionedParameters(ps)
      val filter = args.flatMap(_.filter)
      distinctModelIds.foreach { id =>
        pp.setGcValue(id)
        filter.foreach { filter =>
          SetParams.setParams(pp, filter)
        }
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
      val builder = RelationQueryBuilder(schemaName, relation, args)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      SetParams.setQueryArgs(ps, args)
      val rs: ResultSet = ps.executeQuery()

      val result = rs.as(readRelation(relation))

      ResolverResult(result)
    }
  }

  def selectAllFromListTable(
      model: Model,
      field: ScalarField,
      args: Option[QueryArguments]
  ): DBIO[ResolverResult[ScalarListValues]] = {

    SimpleDBIO[ResolverResult[ScalarListValues]] { ctx =>
      val builder = ScalarListQueryBuilder(schemaName, field, args)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      SetParams.setQueryArgs(ps, args)
      val rs: ResultSet = ps.executeQuery()

      val result = rs.as(readsScalarListField(field))

      val convertedValues = result
        .groupBy(_.nodeId)
        .map { case (id, values) => ScalarListValues(IdGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }
        .toVector

      ResolverResult(convertedValues)
    }
  }

  def selectFromScalarList(modelName: String, field: ScalarField, nodeIds: Vector[IdGCValue]): DBIO[Vector[ScalarListValues]] = {
    SimpleDBIO[Vector[ScalarListValues]] { ctx =>
      val placeHolders = queryPlaceHolders(nodeIds)
      val q            = s"""select "nodeId", "position", "value" from "$schemaName"."${modelName}_${field.dbName}" where "nodeId" in """ + placeHolders
      val ps           = ctx.connection.prepareStatement(q)
      val pp           = new PositionedParameters(ps)
      nodeIds.foreach(pp.setGcValue)

      val rs                 = ps.executeQuery()
      val scalarListElements = rs.as(readsScalarListField(field))

      val grouped: Map[Id, Vector[ScalarListElement]] = scalarListElements.groupBy(_.nodeId)
      grouped.map { case (id, values) => ScalarListValues(IdGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }.toVector
    }
  }

  def countAllFromTable(table: String, whereFilter: Option[Filter]): DBIO[Int] = {
    SimpleDBIO[Int] { ctx =>
      val builder = CountQueryBuilder(schemaName, table, whereFilter)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      SetParams.setFilter(ps, whereFilter)
      val rs = ps.executeQuery()
      rs.next()
      rs.getInt(1)
    }
  }

  def batchSelectFromModelByUnique(model: Model, field: ScalarField, values: Vector[GCValue]): DBIO[Vector[PrismaNode]] = {
    SimpleDBIO { ctx =>
      val queryArgs = Some(QueryArguments.withFilter(ScalarFilter(field, In(values))))
      val builder   = ModelQueryBuilder(schemaName, model, queryArgs)
      val ps        = ctx.connection.prepareStatement(builder.queryString)
      SetParams.setQueryArgs(ps, queryArgs)
      val rs: ResultSet = ps.executeQuery()
      rs.as(readsPrismaNode(model))
    }
  }
}
