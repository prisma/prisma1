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
    val data = model.scalarNonListFields.map(field => field.name -> ps.rs.getGcValue(field.dbName, field.typeIdentifier))

    PrismaNode(id = ps.rs.getId(model), data = RootGCValue(data: _*), Some(model.name))
  }

  def getResultForModelAndRelationSide(model: Model, side: String, oppositeSide: String): GetResult[PrismaNodeWithParent] = GetResult { ps: PositionedResult =>
    val node       = getPrismaNode(model, ps)
    val firstSide  = ps.rs.getParentId(side)
    val secondSide = ps.rs.getParentId(oppositeSide)
    val parentId   = if (firstSide == node.id) secondSide else firstSide

    PrismaNodeWithParent(parentId, node)
  }

  private def getResultForRelation(relation: Relation): GetResult[RelationNode] = GetResult { ps: PositionedResult =>
    val modelAColumn = relation.columnForRelationSide(RelationSide.A)
    val modelBColumn = relation.columnForRelationSide(RelationSide.B)
    RelationNode(a = ps.rs.getAsID(modelAColumn), b = ps.rs.getAsID(modelBColumn))
  }

  implicit object GetRelationCount extends GetResult[(IdGCValue, Int)] {
    override def apply(ps: PositionedResult): (IdGCValue, Int) = (ps.rs.getAsID("id"), ps.rs.getInt("Count"))
  }

  private def whereOrderByLimitCommands(args: Option[QueryArguments],
                                        overrideMaxNodeCount: Option[Int],
                                        tableName: String,
                                        idFieldName: String,
                                        defaultOrderShortCut: Option[String] = None,
                                        forList: Boolean = false) = {
    val (where, orderBy, limit) = extractQueryArgs(schemaName, alias = ALIAS, tableName, idFieldName, args, defaultOrderShortCut, overrideMaxNodeCount, forList)
    sql"" ++ prefixIfNotNone("where", where) ++ prefixIfNotNone("order by", orderBy) ++ prefixIfNotNone("limit", limit)
  }

  private def andWhereOrderByLimitCommands(args: Option[QueryArguments],
                                           overrideMaxNodeCount: Option[Int],
                                           tableName: String,
                                           idFieldName: String,
                                           defaultOrderShortCut: Option[String] = None,
                                           forList: Boolean = false) = {
    val (where, orderBy, limit) = extractQueryArgs(schemaName, alias = ALIAS, tableName, idFieldName, args, defaultOrderShortCut, overrideMaxNodeCount, forList)
    sql"" ++ prefixIfNotNone("and", where) ++ prefixIfNotNone("order by", orderBy) ++ prefixIfNotNone("limit", limit)
  }

  def getResultForScalarListField(field: ScalarField): GetResult[ScalarListElement] = GetResult { ps: PositionedResult =>
    val resultSet = ps.rs
    val nodeId    = resultSet.getString("nodeId")
    val position  = resultSet.getInt("position")
    val value     = resultSet.getGcValue("value", field.typeIdentifier)
    ScalarListElement(nodeId, position, value)
  }

  def selectAllFromTable(
      model: Model,
      args: Option[QueryArguments],
      overrideMaxNodeCount: Option[Int] = None
  ): DBIOAction[ResolverResult[PrismaNode], NoStream, Effect] = {

    val query = sql"""select * from "#$schemaName"."#${model.dbName}" as "#$ALIAS" """ ++ whereOrderByLimitCommands(args,
                                                                                                                    overrideMaxNodeCount,
                                                                                                                    model.dbName,
                                                                                                                    model.dbNameOfIdField_!)

    query.as[PrismaNode](getResultForModel(model)).map(args.get.resultTransform)
  }

  def selectAllFromRelationTable(
      relation: Relation,
      args: Option[QueryArguments],
      overrideMaxNodeCount: Option[Int] = None
  ): DBIOAction[ResolverResult[RelationNode], NoStream, Effect] = {

    val tableName = relation.relationTableName

    val query = sql"""select * from "#$schemaName"."#$tableName" as "#$ALIAS" """ ++ whereOrderByLimitCommands(args,
                                                                                                               overrideMaxNodeCount,
                                                                                                               tableName,
                                                                                                               relation.columnForRelationSide(RelationSide.A))

    query.as[RelationNode](getResultForRelation(relation)).map(args.get.resultTransform)
  }

  def selectAllFromListTable(
      model: Model,
      field: ScalarField,
      args: Option[QueryArguments],
      overrideMaxNodeCount: Option[Int] = None
  ): DBIOAction[ResolverResult[ScalarListValues], NoStream, Effect] = {

    val tableName = s"${model.dbName}_${field.dbName}"
    val query = sql"""select *from "#$schemaName"."#$tableName" "#$ALIAS" """ ++ whereOrderByLimitCommands(args,
                                                                                                           overrideMaxNodeCount,
                                                                                                           tableName,
                                                                                                           model.dbNameOfIdField_!,
                                                                                                           None,
                                                                                                           true)

    query.as[ScalarListElement](getResultForScalarListField(field)).map { scalarListElements =>
      val res = args.get.resultTransform(scalarListElements)
      val convertedValues =
        res.nodes
          .groupBy(_.nodeId)
          .map { case (id, values) => ScalarListValues(IdGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }
          .toVector
      res.copy(nodes = convertedValues)
    }
  }

  def countAllFromTable(table: String, whereFilter: Option[Filter]): DBIOAction[Int, NoStream, Effect] = {
    val query = sql"""select count(*)from "#$schemaName"."#$table" """ ++ whereFilterAppendix(schemaName, table, whereFilter)

    query.as[Int].map(_.head)
  }

  def batchSelectFromModelByUnique(model: Model, fieldName: String, values: Vector[GCValue]): SimpleDBIO[Vector[PrismaNode]] =
    SimpleDBIO[Vector[PrismaNode]] { x =>
      val placeHolders                   = values.map(_ => "?").mkString(",")
      val query                          = s"""select * from "$schemaName"."${model.dbName}" where "$fieldName" in ($placeHolders)"""
      val batchSelect: PreparedStatement = x.connection.prepareStatement(query)
      values.zipWithIndex.foreach { gcValueWithIndex =>
        batchSelect.setGcValue(gcValueWithIndex._2 + 1, gcValueWithIndex._1)
      }
      val rs: ResultSet = batchSelect.executeQuery()

      var result: Vector[PrismaNode] = Vector.empty
      while (rs.next) {
        val data = model.scalarNonListFields.map(field => field.name -> rs.getGcValue(field.dbName, field.typeIdentifier))
        result = result :+ PrismaNode(id = rs.getId(model), data = RootGCValue(data: _*))
      }

      result
    }

  def selectFromScalarList(modelName: String, field: ScalarField, nodeIds: Vector[IdGCValue]): DBIOAction[Vector[ScalarListValues], NoStream, Effect] = {
    val query = sql"""select "nodeId", "position", "value" from "#$schemaName"."#${modelName}_#${field.dbName}" where "nodeId" in (""" ++ combineByComma(
      nodeIds.map(v => sql"$v")) ++ sql")"

    query.as[ScalarListElement](getResultForScalarListField(field)).map { scalarListElements =>
      val grouped: Map[Id, Vector[ScalarListElement]] = scalarListElements.groupBy(_.nodeId)
      grouped.map { case (id, values) => ScalarListValues(IdGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }.toVector
    }
  }

  def batchSelectAllFromRelatedModel(
      schema: Schema,
      fromField: RelationField,
      fromModelIds: Vector[IdGCValue],
      args: Option[QueryArguments]
  ): DBIOAction[Vector[ResolverResult[PrismaNodeWithParent]], NoStream, Effect] = {
    val relation     = fromField.relation
    val relatedModel = fromField.relatedModel_!
    val modelTable   = relatedModel.dbName

    val relationTableName     = fromField.relation.relationTableName
    val (aColumn, bColumn)    = (relation.modelAColumn, relation.modelBColumn)
    val columnForFromModel    = relation.columnForRelationSide(fromField.relationSide)
    val columnForRelatedModel = relation.columnForRelationSide(fromField.oppositeRelationSide)

    def createQuery(id: String, modelRelationSide: String, fieldRelationSide: String) = {
      sql"""(select "#$ALIAS".*, "RelationTable"."#$aColumn" as "__Relation__A",  "RelationTable"."#$bColumn" as "__Relation__B"
            from "#$schemaName"."#$modelTable" as "#$ALIAS"
            inner join "#$schemaName"."#$relationTableName" as "RelationTable"
            on "#$ALIAS"."#${relatedModel.dbNameOfIdField_!}" = "RelationTable"."#$fieldRelationSide"
            where "RelationTable"."#$modelRelationSide" = '#$id' """ ++ andWhereOrderByLimitCommands(
        args,
        None,
        relatedModel.dbName,
        relatedModel.dbNameOfIdField_!,
        Some(s""" "RelationTable"."$columnForRelatedModel" """)) ++ sql")"
    }

    // see https://github.com/graphcool/internal-docs/blob/master/relations.md#findings
    val resolveFromBothSidesAndMerge = fromField.relation.isSameFieldSameModelRelation

    val query = resolveFromBothSidesAndMerge match {
      case false =>
        fromModelIds.distinct.view.zipWithIndex.foldLeft(sql"")((a, b) =>
          a ++ unionIfNotFirst(b._2) ++ createQuery(b._1.value, columnForFromModel, columnForRelatedModel))

      case true =>
        fromModelIds.distinct.view.zipWithIndex.foldLeft(sql"")(
          (a, b) =>
            a ++ unionIfNotFirst(b._2) ++
              createQuery(b._1.value, columnForFromModel, columnForRelatedModel) ++
              sql"union all " ++
              createQuery(b._1.value, columnForRelatedModel, columnForFromModel))
    }

    val modelRelationSide    = fromField.relationSide.toString
    val oppositeRelationSide = fromField.oppositeRelationSide.toString
    query
      .as[PrismaNodeWithParent](getResultForModelAndRelationSide(relatedModel, modelRelationSide, oppositeRelationSide))
      .map { items =>
        val itemGroupsByModelId = items.groupBy(_.parentId)
        fromModelIds.map { id =>
          itemGroupsByModelId.find(_._1 == id) match {
            case Some((_, itemsForId)) => args.get.resultTransform(itemsForId).copy(parentModelId = Some(id))
            case None                  => ResolverResult(Vector.empty[PrismaNodeWithParent], hasPreviousPage = false, hasNextPage = false, parentModelId = Some(id))
          }
        }
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
        None,
        relatedModel.dbName,
        relatedModel.dbNameOfIdField_!,
        Some(s"""$schemaName.$unsafeRelationId.$columnForFieldRelationSide""")) ++ sql")"
    }

    val query = parentNodeIds.distinct.view.zipWithIndex.foldLeft(sql"")((a, b) => a ++ unionIfNotFirst(b._2) ++ createQuery(b._1.value))

    query.as[(IdGCValue, Int)]
  }

  def unionIfNotFirst(index: Int): SQLActionBuilder = if (index == 0) sql"" else sql"union all "

// used in tests only

  def itemCountForTable(projectId: String, modelName: String) = sql"""SELECT COUNT(*) AS Count FROM "#$projectId"."#$modelName"""".as[Int] //todo schemaName??

}
