package com.prisma.api.connector.postgresql.database

import java.sql.ResultSet

import com.prisma.api.connector._
import com.prisma.gc_values._
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Function => _, _}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{SQLActionBuilder, _}

import scala.concurrent.ExecutionContext

case class PostgresApiDatabaseQueryBuilder(
    project: Project,
    schemaName: String
)(implicit ec: ExecutionContext) {
  import JdbcExtensions._
  import PostgresQueryArgumentsExtensions._
  import PostgresSlickExtensions._
  import com.prisma.slick.NewJdbcExtensions._

  def getResultForScalarListField(field: ScalarField): GetResult[ScalarListElement] = GetResult { ps: PositionedResult =>
    readScalarListElement(field, ps.rs)
  }

  def readScalarListElement(field: ScalarField, resultSet: ResultSet): ScalarListElement = {
    val nodeId   = resultSet.getString("nodeId")
    val position = resultSet.getInt("position")
    val value    = resultSet.getGcValue("value", field.typeIdentifier)
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
      val builder = QueryBuilders.model(schemaName, model, args)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      builder.setParamsForQueryArgs(ps, args)
      // execute
      val rs: ResultSet = ps.executeQuery()
      // read result
      val result: Vector[PrismaNode] = rs.as[PrismaNode](readsPrismaNode(model))
      ResolverResult(result, hasNextPage = false, hasPreviousPage = false)
    }
  }

  def batchSelectAllFromRelatedModel(
      schema: Schema,
      fromField: RelationField,
      fromModelIds: Vector[IdGCValue],
      args: Option[QueryArguments]
  ): DBIO[Vector[ResolverResult[PrismaNodeWithParent]]] = {
    SimpleDBIO[Vector[ResolverResult[PrismaNodeWithParent]]] { ctx =>
      val builder = RelatedModelsQueryBuilder(schemaName, fromField, args)
      // see https://github.com/graphcool/internal-docs/blob/master/relations.md#findings
      val resolveFromBothSidesAndMerge = fromField.relation.isSameFieldSameModelRelation

      val baseQuery = if (resolveFromBothSidesAndMerge) {
        "((" + builder.queryString + ") union all " + "(" + builder.queryStringFromOtherSide + "))"
      } else {
        "(" + builder.queryString + ")"
      }
      val distinctModelIds = fromModelIds.distinct

      val queries = Vector.fill(distinctModelIds.size)(baseQuery)
      val query   = queries.mkString(" union all ")

      val ps = ctx.connection.prepareStatement(query)
      println("!!" * 50)
      println(query)

      // injecting params
      val pp     = new PositionedParameters(ps)
      val filter = args.flatMap(_.filter)
      distinctModelIds.foreach { id =>
        pp.setGcValue(id)
        filter.foreach { filter =>
          SetParams.setParams(pp, filter)
        }
        if (resolveFromBothSidesAndMerge) { // each query is repeated so we have to set them a second time
          pp.setGcValue(id)
          filter.foreach { filter =>
            SetParams.setParams(pp, filter)
          }
        }
      }

      // executing
      val rs: ResultSet       = ps.executeQuery()
      val result              = rs.as[PrismaNodeWithParent](readPrismaNodeWithParent(fromField.relatedModel_!, fromField.relationSide, fromField.oppositeRelationSide))
      val itemGroupsByModelId = result.groupBy(_.parentId)
      fromModelIds.map { id =>
        itemGroupsByModelId.find(_._1 == id) match {
          case Some((_, itemsForId)) => ResolverResult(itemsForId, hasPreviousPage = false, hasNextPage = false, parentModelId = Some(id))
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
      val builder = QueryBuilders.relation(schemaName, relation, args)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      builder.setParamsForQueryArgs(ps, args)
      val rs: ResultSet = ps.executeQuery()

      val result = rs.as(readRelation(relation))

      ResolverResult(result, hasNextPage = false, hasPreviousPage = false)
    }
  }

  def selectAllFromListTable(
      model: Model,
      field: ScalarField,
      args: Option[QueryArguments]
  ): DBIO[ResolverResult[ScalarListValues]] = {

    SimpleDBIO[ResolverResult[ScalarListValues]] { ctx =>
      val builder = QueryBuilders.scalarList(schemaName, field, args)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      builder.setParamsForQueryArgs(ps, args)
      val rs: ResultSet = ps.executeQuery()

      var result: Vector[ScalarListElement] = Vector.empty
      while (rs.next) {
        result = result :+ readScalarListElement(field, rs)
      }

      val convertedValues = result
        .groupBy(_.nodeId)
        .map { case (id, values) => ScalarListValues(IdGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }
        .toVector

      ResolverResult(convertedValues, hasNextPage = false, hasPreviousPage = false)
    }
  }

  def selectFromScalarList(modelName: String, field: ScalarField, nodeIds: Vector[IdGCValue]): DBIO[Vector[ScalarListValues]] = {
    val query = sql"""select "nodeId", "position", "value" from "#$schemaName"."#${modelName}_#${field.dbName}" where "nodeId" in (""" ++ combineByComma(
      nodeIds.map(v => sql"$v")) ++ sql")"

    query.as[ScalarListElement](getResultForScalarListField(field)).map { scalarListElements =>
      val grouped: Map[Id, Vector[ScalarListElement]] = scalarListElements.groupBy(_.nodeId)
      grouped.map { case (id, values) => ScalarListValues(IdGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }.toVector
    }
  }

  def countAllFromTable(table: String, whereFilter: Option[Filter]): DBIO[Int] = {
    SimpleDBIO[Int] { ctx =>
      val builder = CountQueryBuilder(schemaName, table, whereFilter)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      builder.setParamsForFilter(ps, whereFilter)
      val rs = ps.executeQuery()
      rs.next()
      rs.getInt(1)
    }
  }

  def batchSelectFromModelByUnique(model: Model, field: ScalarField, values: Vector[GCValue]): DBIO[Vector[PrismaNode]] = {
    SimpleDBIO { ctx =>
      val queryArgs = Some(QueryArguments.withFilter(ScalarFilter(field, In(values))))
      val builder   = QueryBuilders.model(schemaName, model, queryArgs)
      val ps        = ctx.connection.prepareStatement(builder.queryString)
      builder.setParamsForQueryArgs(ps, queryArgs)
      val rs: ResultSet = ps.executeQuery()
      rs.as(readsPrismaNode(model))
    }
  }
}
