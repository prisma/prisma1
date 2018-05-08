package com.prisma.api.connector.postgresql.database

import java.sql.{PreparedStatement, ResultSet}

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.api.connector._
import com.prisma.gc_values._
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Function => _, _}
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{SQLActionBuilder, _}
import slick.sql.SqlStreamingAction

import scala.concurrent.ExecutionContext.Implicits.global

case class PostgresApiDatabaseQueryBuilder(
    schemaName: String
) {
  import JdbcExtensions._
  import QueryArgumentsExtensions._
  import SlickExtensions._

  def getResultForModel(model: Model): GetResult[PrismaNode] = GetResult { ps: PositionedResult =>
    getPrismaNode(model, ps)
  }

  private def getPrismaNode(model: Model, ps: PositionedResult) = {
    val data = model.scalarNonListFields.map(field => field.name -> ps.rs.getGcValue(field.name, field.typeIdentifier))

    PrismaNode(id = ps.rs.getId, data = RootGCValue(data: _*))
  }

  def getResultForModelAndRelationSide(model: Model, side: String, oppositeSide: String): GetResult[PrismaNodeWithParent] = GetResult { ps: PositionedResult =>
    val node       = getPrismaNode(model, ps)
    val firstSide  = ps.rs.getParentId(side)
    val secondSide = ps.rs.getParentId(oppositeSide)
    val parentId   = if (firstSide == node.id) secondSide else firstSide

    PrismaNodeWithParent(parentId, node)
  }

  implicit object GetRelationNode extends GetResult[RelationNode] {
    override def apply(ps: PositionedResult): RelationNode = RelationNode(ps.rs.getId, ps.rs.getAsID("A"), ps.rs.getAsID("B"))

  }

  implicit object GetRelationCount extends GetResult[(IdGCValue, Int)] {
    override def apply(ps: PositionedResult): (IdGCValue, Int) = (ps.rs.getId, ps.rs.getInt("Count"))
  }

  def getResultForScalarListField(field: Field): GetResult[ScalarListElement] = GetResult { ps: PositionedResult =>
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

    val tableName                                        = model.dbName
    val (conditionCommand, orderByCommand, limitCommand) = extractQueryArgs(schemaName, tableName, args, None, overrideMaxNodeCount = overrideMaxNodeCount)

    val query = sql"""select * from "#$schemaName"."#$tableName"""" ++
      prefixIfNotNone("where", conditionCommand) ++
      prefixIfNotNone("order by", orderByCommand) ++
      prefixIfNotNone("limit", limitCommand)

    query.as[PrismaNode](getResultForModel(model)).map(args.get.resultTransform)
  }

  def selectAllFromRelationTable(
      relationId: String,
      args: Option[QueryArguments],
      overrideMaxNodeCount: Option[Int] = None
  ): DBIOAction[ResolverResult[RelationNode], NoStream, Effect] = {

    val tableName                                        = relationId
    val (conditionCommand, orderByCommand, limitCommand) = extractQueryArgs(schemaName, tableName, args, None, overrideMaxNodeCount = overrideMaxNodeCount)

    val query = sql"""select * from "#$schemaName"."#$tableName"""" ++
      prefixIfNotNone("where", conditionCommand) ++
      prefixIfNotNone("order by", orderByCommand) ++
      prefixIfNotNone("limit", limitCommand)

    query.as[RelationNode].map(args.get.resultTransform)
  }

  def selectAllFromListTable(
      model: Model,
      field: Field,
      args: Option[QueryArguments],
      overrideMaxNodeCount: Option[Int] = None
  ): DBIOAction[ResolverResult[ScalarListValues], NoStream, Effect] = {

    val tableName = s"${model.name}_${field.name}"
    val (conditionCommand, orderByCommand, limitCommand) =
      extractQueryArgs(schemaName, tableName, args, None, overrideMaxNodeCount = overrideMaxNodeCount, true)

    val query =
      sql"""select * from "#$schemaName"."#$tableName"""" ++
        prefixIfNotNone("where", conditionCommand) ++
        prefixIfNotNone("order by", orderByCommand) ++
        prefixIfNotNone("limit", limitCommand)

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

  def countAllFromTable(table: String, whereFilter: Option[DataItemFilterCollection]): DBIOAction[Int, NoStream, Effect] = {
    val query = sql"""select count(*) from "#$schemaName"."#$table"""" ++ whereFilterAppendix(schemaName, table, whereFilter)
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
        result = result :+ PrismaNode(id = rs.getId, data = RootGCValue(data: _*))
      }

      result
    }

  def selectFromScalarList(modelName: String, field: Field, nodeIds: Vector[IdGCValue]): DBIOAction[Vector[ScalarListValues], NoStream, Effect] = {
    val query = sql"""select "nodeId", "position", "value" from "#$schemaName"."#${modelName}_#${field.name}" where "nodeId" in (""" ++ combineByComma(
      nodeIds.map(v => sql"$v")) ++ sql")"

    query.as[ScalarListElement](getResultForScalarListField(field)).map { scalarListElements =>
      val grouped: Map[Id, Vector[ScalarListElement]] = scalarListElements.groupBy(_.nodeId)
      grouped.map {
        case (id, values) =>
          val gcValues = values.sortBy(_.position).map(_.value)
          ScalarListValues(IdGCValue(id), ListGCValue(gcValues))
      }.toVector
    }
  }

  def batchSelectAllFromRelatedModel(
      schema: Schema,
      fromField: Field,
      fromModelIds: Vector[IdGCValue],
      args: Option[QueryArguments]
  ): DBIOAction[Vector[ResolverResult[PrismaNodeWithParent]], NoStream, Effect] = {

    val relatedModel         = fromField.relatedModel(schema).get
    val fieldTable           = fromField.relatedModel(schema).get.name
    val unsafeRelationId     = fromField.relation.get.relationTableName
    val modelRelationSide    = fromField.relationSide.get.toString
    val oppositeRelationSide = fromField.oppositeRelationSide.get.toString

    val (conditionCommand, orderByCommand, limitCommand) =
      extractQueryArgs(schemaName, fieldTable, args, defaultOrderShortcut = Some(s""" RelationTable."$oppositeRelationSide" """), None)

    def createQuery(id: String, modelRelationSide: String, fieldRelationSide: String) = {
      sql"""(select ModelTable.*, RelationTable."A" as __Relation__A,  RelationTable."B" as __Relation__B
            from "#$schemaName"."#$fieldTable" as ModelTable
           inner join "#$schemaName"."#$unsafeRelationId" as RelationTable
           on ModelTable."id" = RelationTable."#$fieldRelationSide"
           where RelationTable."#$modelRelationSide" = '#$id' """ ++
        prefixIfNotNone("and", conditionCommand) ++
        prefixIfNotNone("order by", orderByCommand) ++
        prefixIfNotNone("limit", limitCommand) ++ sql")"
    }

    // see https://github.com/graphcool/internal-docs/blob/master/relations.md#findings
    val resolveFromBothSidesAndMerge = fromField.relation.get.isSameFieldSameModelRelation(schema)

    val query = resolveFromBothSidesAndMerge match {
      case false =>
        fromModelIds.distinct.view.zipWithIndex.foldLeft(sql"")((a, b) =>
          a ++ unionIfNotFirst(b._2) ++ createQuery(b._1.value, modelRelationSide, oppositeRelationSide))

      case true =>
        fromModelIds.distinct.view.zipWithIndex.foldLeft(sql"")((a, b) =>
          a ++ unionIfNotFirst(b._2) ++ createQuery(b._1.value, modelRelationSide, oppositeRelationSide) ++ sql"union all " ++ createQuery(b._1.value,
                                                                                                                                           oppositeRelationSide,
                                                                                                                                           modelRelationSide))
    }

    query
      .as[PrismaNodeWithParent](getResultForModelAndRelationSide(relatedModel, modelRelationSide, oppositeRelationSide))
      .map { items =>
        val itemGroupsByModelId = items.groupBy(_.parentId)
        fromModelIds
          .map(id =>
            itemGroupsByModelId.find(_._1 == id) match {
              case Some((_, itemsForId)) => args.get.resultTransform(itemsForId).copy(parentModelId = Some(id))
              case None                  => ResolverResult(Vector.empty[PrismaNodeWithParent], hasPreviousPage = false, hasNextPage = false, parentModelId = Some(id))
          })
      }
  }

  def countAllFromRelatedModels(
      schema: Schema,
      relationField: Field,
      parentNodeIds: Vector[IdGCValue],
      args: Option[QueryArguments]
  ): SqlStreamingAction[Vector[(IdGCValue, Int)], (IdGCValue, Int), Effect] = {

    val fieldTable        = relationField.relatedModel(schema).get.name
    val unsafeRelationId  = relationField.relation.get.relationTableName
    val modelRelationSide = relationField.relationSide.get.toString
    val fieldRelationSide = relationField.oppositeRelationSide.get.toString

    val (conditionCommand, orderByCommand, limitCommand) =
      extractQueryArgs(schemaName, fieldTable, args, defaultOrderShortcut = Some(s"""$schemaName.$unsafeRelationId.$fieldRelationSide"""), None)

    def createQuery(id: String) = {
      sql"""(select "#$id", count(*) from "#$schemaName"."#$fieldTable"
           inner join "#$schemaName"."#$unsafeRelationId"
           on "#$schemaName"."#$fieldTable"."id" = "#$schemaName"."#$unsafeRelationId"."#$fieldRelationSide"
           where "#$schemaName"."#$unsafeRelationId"."#$modelRelationSide" = '#$id' """ ++
        prefixIfNotNone("and", conditionCommand) ++
        prefixIfNotNone("order by", orderByCommand) ++
        prefixIfNotNone("limit", limitCommand) ++ sql")"
    }

    val query = parentNodeIds.distinct.view.zipWithIndex.foldLeft(sql"")((a, b) => a ++ unionIfNotFirst(b._2) ++ createQuery(b._1.value))

    query.as[(IdGCValue, Int)]
  }

  def unionIfNotFirst(index: Int): SQLActionBuilder = if (index == 0) sql"" else sql"union all "

// used in tests only

  def itemCountForTable(projectId: String, modelName: String) = sql"""SELECT COUNT(*) AS Count FROM "#$projectId"."#$modelName"""".as[Int]

}
