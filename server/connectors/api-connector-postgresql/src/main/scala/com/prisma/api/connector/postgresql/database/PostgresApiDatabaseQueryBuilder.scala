package com.prisma.api.connector.postgresql.database

import java.sql.{PreparedStatement, ResultSet}

import com.prisma.api.connector._
import com.prisma.gc_values._
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Function => _, _}
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{SQLActionBuilder, _}
import slick.sql.SqlStreamingAction

import scala.concurrent.ExecutionContext

case class PostgresApiDatabaseQueryBuilder(
    project: Project,
    schemaName: String
)(implicit ec: ExecutionContext) {
  import JdbcExtensions._
  import PostgresQueryArgumentsExtensions._
  import PostgresSlickExtensions._

  def getResultForModel(model: Model): GetResult[PrismaNode] = GetResult { ps: PositionedResult =>
    getPrismaNode(model, ps)
  }

  private def getPrismaNode(model: Model, ps: PositionedResult) = {
    readPrismaNode(model, ps.rs)
  }

  private def readPrismaNode(model: Model, rs: ResultSet) = {
    val data = model.scalarNonListFields.map(field => field.name -> rs.getGcValue(field.dbName, field.typeIdentifier))
    PrismaNode(id = rs.getId(model), data = RootGCValue(data: _*))
  }

  def getResultForModelAndRelationSide(model: Model, side: String, oppositeSide: String): GetResult[PrismaNodeWithParent] = GetResult { ps: PositionedResult =>
    val node       = getPrismaNode(model, ps)
    val firstSide  = ps.rs.getParentId(side)
    val secondSide = ps.rs.getParentId(oppositeSide)
    val parentId   = if (firstSide == node.id) secondSide else firstSide

    PrismaNodeWithParent(parentId, node)
  }

  def readPrismaNodeWithParent(model: Model, side: String, oppositeSide: String, rs: ResultSet): PrismaNodeWithParent = {
    val node       = readPrismaNode(model, rs)
    val firstSide  = rs.getParentId(side)
    val secondSide = rs.getParentId(oppositeSide)
    val parentId   = if (firstSide == node.id) secondSide else firstSide

    PrismaNodeWithParent(parentId, node)
  }
//
//  private def getResultForRelation(relation: Relation): GetResult[RelationNode] = GetResult { ps: PositionedResult =>
//    val modelAColumn = relation.columnForRelationSide(RelationSide.A)
//    val modelBColumn = relation.columnForRelationSide(RelationSide.B)
//    RelationNode(a = ps.rs.getAsID(modelAColumn), b = ps.rs.getAsID(modelBColumn))
//  }

  private def readRelation(relation: Relation, resultSet: ResultSet): RelationNode = {
    val modelAColumn = relation.columnForRelationSide(RelationSide.A)
    val modelBColumn = relation.columnForRelationSide(RelationSide.B)
    RelationNode(a = resultSet.getAsID(modelAColumn), b = resultSet.getAsID(modelBColumn))
  }

  implicit object GetRelationCount extends GetResult[(IdGCValue, Int)] {
    override def apply(ps: PositionedResult): (IdGCValue, Int) = (ps.rs.getAsID("id"), ps.rs.getInt("Count"))
  }

  private def whereOrderByLimitCommands(args: Option[QueryArguments],
                                        tableName: String,
                                        idFieldName: String,
                                        defaultOrderShortCut: Option[String] = None,
                                        forList: Boolean = false) = {
    val (where, orderBy, limit) = extractQueryArgs(schemaName, alias = ALIAS, tableName, idFieldName, args, defaultOrderShortCut, forList)
    sql"" ++ prefixIfNotNone("where", where) ++ prefixIfNotNone("order by", orderBy) ++ prefixIfNotNone("limit", limit)
  }

  private def andWhereOrderByLimitCommands(args: Option[QueryArguments],
                                           tableName: String,
                                           idFieldName: String,
                                           defaultOrderShortCut: Option[String] = None,
                                           forList: Boolean = false) = {
    val (where, orderBy, limit) = extractQueryArgs(schemaName, alias = ALIAS, tableName, idFieldName, args, defaultOrderShortCut, forList)
    sql"" ++ prefixIfNotNone("and", where) ++ prefixIfNotNone("order by", orderBy) ++ prefixIfNotNone("limit", limit)
  }

  def getResultForScalarListField(field: ScalarField): GetResult[ScalarListElement] = GetResult { ps: PositionedResult =>
    readScalarListElement(field, ps.rs)
  }

  def readScalarListElement(field: ScalarField, resultSet: ResultSet): ScalarListElement = {
    val nodeId   = resultSet.getString("nodeId")
    val position = resultSet.getInt("position")
    val value    = resultSet.getGcValue("value", field.typeIdentifier)
    ScalarListElement(nodeId, position, value)
  }

  def selectAllFromTable(
      model: Model,
      args: Option[QueryArguments],
      overrideMaxNodeCount: Option[Int] = None
  ): DBIO[ResolverResult[PrismaNode]] = {

//    val query = sql"""select * from "#$schemaName"."#${model.dbName}" as "#$ALIAS" """ ++ whereOrderByLimitCommands(args,
//                                                                                                                    overrideMaxNodeCount,
//                                                                                                                    model.dbName,
//                                                                                                                    model.dbNameOfIdField_!)

    SimpleDBIO[ResolverResult[PrismaNode]] { ctx =>
      val builder = QueryBuilders.model(schemaName, model, args)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      builder.setParamsForQueryArgs(ps, args)
      val rs: ResultSet = ps.executeQuery()

      var result: Vector[PrismaNode] = Vector.empty
      while (rs.next) {
        val data = model.scalarNonListFields.map(field => field.name -> rs.getGcValue(field.dbName, field.typeIdentifier))
        result = result :+ PrismaNode(id = rs.getId(model), data = RootGCValue(data: _*))
      }

      ResolverResult(result, hasNextPage = false, hasPreviousPage = false)
    }

//    query.as[PrismaNode](getResultForModel(model)).map(args.get.resultTransform)
  }

  def batchSelectAllFromRelatedModel(
      schema: Schema,
      fromField: RelationField,
      fromModelIds: Vector[IdGCValue],
      args: Option[QueryArguments]
  ): DBIO[Vector[ResolverResult[PrismaNodeWithParent]]] = {
//    val relation     = fromField.relation
//    val relatedModel = fromField.relatedModel_!
//    val modelTable   = relatedModel.dbName
//
//    val relationTableName     = fromField.relation.relationTableName
//    val (aColumn, bColumn)    = (relation.modelAColumn, relation.modelBColumn)
//    val columnForFromModel    = relation.columnForRelationSide(fromField.relationSide)
//    val columnForRelatedModel = relation.columnForRelationSide(fromField.oppositeRelationSide)
//
//    def createQuery(id: String, modelRelationSide: String, fieldRelationSide: String) = {
//      sql"""(select "#$ALIAS".*, "RelationTable"."#$aColumn" as "__Relation__A",  "RelationTable"."#$bColumn" as "__Relation__B"
//            from "#$schemaName"."#$modelTable" as "#$ALIAS"
//            inner join "#$schemaName"."#$relationTableName" as "RelationTable"
//            on "#$ALIAS"."#${relatedModel.dbNameOfIdField_!}" = "RelationTable"."#$fieldRelationSide"
//            where "RelationTable"."#$modelRelationSide" = '#$id' """ ++ andWhereOrderByLimitCommands(
//        args,
//        relatedModel.dbName,
//        relatedModel.dbNameOfIdField_!,
//        Some(s""" "RelationTable"."$columnForRelatedModel" """)) ++ sql")"
//    }
//
//    // see https://github.com/graphcool/internal-docs/blob/master/relations.md#findings
//    val resolveFromBothSidesAndMerge = fromField.relation.isSameFieldSameModelRelation
//
//    val query = resolveFromBothSidesAndMerge match {
//      case false =>
//        fromModelIds.distinct.view.zipWithIndex.foldLeft(sql"")((a, b) =>
//          a ++ unionIfNotFirst(b._2) ++ createQuery(b._1.value, columnForFromModel, columnForRelatedModel))
//
//      case true =>
//        fromModelIds.distinct.view.zipWithIndex.foldLeft(sql"")(
//          (a, b) =>
//            a ++ unionIfNotFirst(b._2) ++
//              createQuery(b._1.value, columnForFromModel, columnForRelatedModel) ++
//              sql"union all " ++
//              createQuery(b._1.value, columnForRelatedModel, columnForFromModel))
//    }
//
//    val modelRelationSide    = fromField.relationSide.toString
//    val oppositeRelationSide = fromField.oppositeRelationSide.toString

    SimpleDBIO[Vector[ResolverResult[PrismaNodeWithParent]]] { ctx =>
      val builder               = RelatedModelQueryBuilder(schemaName, fromField, args)
      val relation              = fromField.relation
      val columnForFromModel    = relation.columnForRelationSide(fromField.relationSide)
      val columnForRelatedModel = relation.columnForRelationSide(fromField.oppositeRelationSide)
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
      val rs: ResultSet = ps.executeQuery()

      var result: Vector[PrismaNodeWithParent] = Vector.empty
      val model                                = fromField.relatedModel_!
      while (rs.next) {
        val prismaNode = readPrismaNodeWithParent(model, columnForFromModel, columnForRelatedModel, rs)
        result = result :+ prismaNode
      }
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

      var result: Vector[RelationNode] = Vector.empty
      while (rs.next) {
        result = result :+ readRelation(relation, rs)
      }

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
      val filter    = ScalarFilter(field, In(values))
      val queryArgs = Some(QueryArguments.withFilter(filter))
      val builder   = QueryBuilders.model(schemaName, model, queryArgs)
      val ps        = ctx.connection.prepareStatement(builder.queryString)
      builder.setParamsForQueryArgs(ps, queryArgs)
      val rs: ResultSet = ps.executeQuery()

      var result: Vector[PrismaNode] = Vector.empty
      while (rs.next) {
        val data = model.scalarNonListFields.map(field => field.name -> rs.getGcValue(field.dbName, field.typeIdentifier))
        result = result :+ PrismaNode(id = rs.getId(model), data = RootGCValue(data: _*))
      }

      result
    }
  }

  def countAllFromRelatedModels(
      schema: Schema,
      relationField: RelationField,
      parentNodeIds: Vector[IdGCValue],
      args: Option[QueryArguments]
  ): SqlStreamingAction[Vector[(IdGCValue, Int)], (IdGCValue, Int), Effect] = {

    val relatedModel               = relationField.relatedModel_!
    val relation                   = relationField.relation
    val unsafeRelationId           = relation.relationTableName
    val modelRelationSide          = relationField.relationSide.toString
    val columnForFieldRelationSide = relation.columnForRelationSide(relationField.oppositeRelationSide)

    def createQuery(id: String) = {
      sql"""(select "#$id", count(*)
            from "#$schemaName"."#${relatedModel.dbName}" AS "#$ALIAS"
            inner join "#$schemaName"."#$unsafeRelationId"
            on "#$ALIAS"."#${relatedModel.dbNameOfIdField_!}" = "#$schemaName"."#$unsafeRelationId"."#$columnForFieldRelationSide"
            where "#$schemaName"."#$unsafeRelationId"."#$modelRelationSide" = '#$id' """ ++ andWhereOrderByLimitCommands(
        args,
        relatedModel.dbName,
        relatedModel.dbNameOfIdField_!,
        Some(s"""$schemaName.$unsafeRelationId.$columnForFieldRelationSide""")) ++ sql")"
    }

    val query = parentNodeIds.distinct.view.zipWithIndex.foldLeft(sql"")((a, b) => a ++ unionIfNotFirst(b._2) ++ createQuery(b._1.value))

    query.as[(IdGCValue, Int)]
  }

  private def unionIfNotFirst(index: Int): SQLActionBuilder = if (index == 0) sql"" else sql"union all "

}
