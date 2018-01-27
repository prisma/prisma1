package com.prisma.api.database

import com.prisma.api.database.Types.DataItemFilterCollection
import com.prisma.api.mutations.{NodeSelector, ParentInfo}
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Field, Model, Project, Relation}
import slick.dbio.DBIOAction
import slick.dbio.Effect.Read
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.{DatabaseMeta, MTable}
import slick.jdbc.{SQLActionBuilder, _}

import scala.concurrent.ExecutionContext.Implicits.global

object DatabaseQueryBuilder {

  import SlickExtensions._

  implicit object GetDataItem extends GetResult[DataItem] {
    def apply(ps: PositionedResult): DataItem = {
      val rs = ps.rs
      val md = rs.getMetaData
      val colNames = for (i <- 1 to md.getColumnCount)
        yield md.getColumnName(i)

      val userData = (for (n <- colNames.filter(_ != "id"))
        // note: getObject(string) is case insensitive, so we get the index in scala land instead
        yield n -> Option(rs.getObject(colNames.indexOf(n) + 1))).toMap

      DataItem(id = rs.getString("id").trim, userData = userData)
    }
  }

  implicit object GetScalarListValue extends GetResult[ScalarListValue] {
    def apply(ps: PositionedResult): ScalarListValue = {
      val rs = ps.rs

      ScalarListValue(nodeId = rs.getString("nodeId").trim, position = rs.getInt("position"), value = rs.getObject("value"))
    }
  }

  def selectAllFromTable(projectId: String,
                         tableName: String,
                         args: Option[QueryArguments],
                         overrideMaxNodeCount: Option[Int] = None): (SQLActionBuilder, ResultTransform) = {

    val (conditionCommand, orderByCommand, limitCommand, resultTransform) =
      extractQueryArgs(projectId, tableName, args, overrideMaxNodeCount = overrideMaxNodeCount)

    val query =
      sql"select * from `#$projectId`.`#$tableName`" concat
        prefixIfNotNone("where", conditionCommand) concat
        prefixIfNotNone("order by", orderByCommand) concat
        prefixIfNotNone("limit", limitCommand)

    (query, resultTransform)
  }

  def selectAllFromListTable(projectId: String,
                             tableName: String,
                             args: Option[QueryArguments],
                             overrideMaxNodeCount: Option[Int] = None): (SQLActionBuilder, ResultListTransform) = {

    val (conditionCommand, orderByCommand, limitCommand, resultTransform) =
      extractListQueryArgs(projectId, tableName, args, overrideMaxNodeCount = overrideMaxNodeCount)

    val query =
      sql"select * from `#$projectId`.`#$tableName`" concat
        prefixIfNotNone("where", conditionCommand) concat
        prefixIfNotNone("order by", orderByCommand) concat
        prefixIfNotNone("limit", limitCommand)

    (query, resultTransform)
  }

  def countAllFromModel(project: Project, model: Model, where: Option[DataItemFilterCollection]): SQLActionBuilder = {
    val whereSql = where.flatMap { where =>
      QueryArguments.generateFilterConditions(project.id, model.name, where)
    }
    sql"select count(*) from `#${project.id}`.`#${model.name}`" ++ prefixIfNotNone("where", whereSql)
  }

  def extractQueryArgs(
      projectId: String,
      modelName: String,
      args: Option[QueryArguments],
      defaultOrderShortcut: Option[String] = None,
      overrideMaxNodeCount: Option[Int] = None): (Option[SQLActionBuilder], Option[SQLActionBuilder], Option[SQLActionBuilder], ResultTransform) = {
    args match {
      case None => (None, None, None, x => ResolverResult(x))
      case Some(givenArgs: QueryArguments) =>
        (
          givenArgs.extractWhereConditionCommand(projectId, modelName),
          givenArgs.extractOrderByCommand(projectId, modelName, defaultOrderShortcut),
          overrideMaxNodeCount match {
            case None                => givenArgs.extractLimitCommand(projectId, modelName)
            case Some(maxCount: Int) => givenArgs.extractLimitCommand(projectId, modelName, maxCount)
          },
          givenArgs.extractResultTransform(projectId, modelName)
        )
    }
  }

  def extractListQueryArgs(
      projectId: String,
      modelName: String,
      args: Option[QueryArguments],
      defaultOrderShortcut: Option[String] = None,
      overrideMaxNodeCount: Option[Int] = None): (Option[SQLActionBuilder], Option[SQLActionBuilder], Option[SQLActionBuilder], ResultListTransform) = {
    args match {
      case None =>
        (None,
         None,
         None,
         x =>
           ResolverResult(x.map { listValue =>
             DataItem(id = listValue.nodeId, userData = Map("value" -> Some(listValue.value)))
           }))
      case Some(givenArgs: QueryArguments) =>
        (
          givenArgs.extractWhereConditionCommand(projectId, modelName),
          givenArgs.extractOrderByCommandForLists(projectId, modelName, defaultOrderShortcut),
          overrideMaxNodeCount match {
            case None                => givenArgs.extractLimitCommand(projectId, modelName)
            case Some(maxCount: Int) => givenArgs.extractLimitCommand(projectId, modelName, maxCount)
          },
          givenArgs.extractListResultTransform(projectId, modelName)
        )
    }
  }

  def itemCountForTable(projectId: String, modelName: String) = {
    sql"SELECT COUNT(*) AS Count FROM `#$projectId`.`#$modelName`"
  }

  def existsNullByModelAndScalarField(projectId: String, modelName: String, fieldName: String) = {
    sql"""SELECT EXISTS(Select `id` FROM `#$projectId`.`#$modelName`
          WHERE `#$projectId`.`#$modelName`.#$fieldName IS NULL)"""
  }

  def existsNullByModelAndRelationField(projectId: String, modelName: String, field: Field) = {
    val relationId   = field.relation.get.id
    val relationSide = field.relationSide.get.toString
    sql"""select EXISTS (
            select `id`from `#$projectId`.`#$modelName`
            where `id` Not IN
            (Select `#$projectId`.`#$relationId`.#$relationSide from `#$projectId`.`#$relationId`)
          )"""
  }

  def existsNodeIsInRelationshipWith(project: Project, parentInfo: ParentInfo, where: NodeSelector) = {
    val relationSide         = parentInfo.relation.sideOf(where.model).toString
    val oppositeRelationSide = parentInfo.relation.oppositeSideOf(where.model).toString
    sql"""select EXISTS (
            select `id`from `#${project.id}`.`#${where.model.name}`
            where  `#${where.field.name}` = ${where.fieldValue} and `id` IN (
             select `#$relationSide`
             from `#${project.id}`.`#${parentInfo.relation.id}`
             where `#$oppositeRelationSide` = (select `id` from `#${project.id}`.`#${parentInfo.model.name}` where `#${parentInfo.where.field.name}` = ${parentInfo.where.fieldValue})
           )
          )"""
  }

  def existsByModelAndId(projectId: String, modelName: String, id: String) = {
    sql"select exists (select `id` from `#$projectId`.`#$modelName` where `id` = '#$id')"
  }

  def existsByModel(projectId: String, modelName: String): SQLActionBuilder = {
    sql"select exists (select `id` from `#$projectId`.`#$modelName`)"
  }

  def batchSelectFromModelByUnique(projectId: String, modelName: String, key: String, values: List[Any]): SQLActionBuilder = {
    sql"select * from `#$projectId`.`#$modelName` where `#$key` in (" concat combineByComma(values.map(escapeUnsafeParam)) concat sql")"
  }

  def selectFromModelsByUniques(project: Project, model: Model, predicates: Vector[NodeSelector]) = {
    sql"select * from `#${project.id}`.`#${model.name}`" ++ whereClauseByCombiningPredicatesByOr(predicates)
  }

  def existsByWhere(projectId: String, where: NodeSelector) = {
    sql"select exists (select `id` from `#$projectId`.`#${where.model.name}` where  #${where.field.name} = ${where.fieldValue})"
  }

  def selectFromScalarList(projectId: String, modelName: String, fieldName: String, nodeIds: Vector[String]): SQLActionBuilder = {
    sql"select nodeId, position, value from `#$projectId`.`#${modelName}_#$fieldName` where nodeId in (" concat combineByComma(nodeIds.map(escapeUnsafeParam)) concat sql")"
  }

  def whereClauseByCombiningPredicatesByOr(predicates: Vector[NodeSelector]) = {
    if (predicates.isEmpty) {
      sql""
    } else {
      val firstPredicate = predicates.head
      predicates.tail.foldLeft(sql"where #${firstPredicate.field.name} = ${firstPredicate.fieldValue}") { (sqlActionBuilder, predicate) =>
        sqlActionBuilder ++ sql" OR #${predicate.field.name} = ${predicate.fieldValue}"
      }
    }
  }

  def batchSelectAllFromRelatedModel(project: Project,
                                     relationField: Field,
                                     parentNodeIds: List[String],
                                     args: Option[QueryArguments]): (SQLActionBuilder, ResultTransform) = {

    val fieldTable        = relationField.relatedModel(project.schema).get.name
    val unsafeRelationId  = relationField.relation.get.id
    val modelRelationSide = relationField.relationSide.get.toString
    val fieldRelationSide = relationField.oppositeRelationSide.get.toString

    val (conditionCommand, orderByCommand, limitCommand, resultTransform) =
      extractQueryArgs(project.id, fieldTable, args, defaultOrderShortcut = Some(s"""`${project.id}`.`$unsafeRelationId`.$fieldRelationSide"""))

    def createQuery(id: String, modelRelationSide: String, fieldRelationSide: String) = {
      sql"""(select * from `#${project.id}`.`#$fieldTable`
           inner join `#${project.id}`.`#$unsafeRelationId`
           on `#${project.id}`.`#$fieldTable`.id = `#${project.id}`.`#$unsafeRelationId`.#$fieldRelationSide
           where `#${project.id}`.`#$unsafeRelationId`.#$modelRelationSide = '#$id' """ concat
        prefixIfNotNone("and", conditionCommand) concat
        prefixIfNotNone("order by", orderByCommand) concat
        prefixIfNotNone("limit", limitCommand) concat sql")"
    }

    def unionIfNotFirst(index: Int): SQLActionBuilder =
      if (index == 0) {
        sql""
      } else {
        sql"union all "
      }

    // see https://github.com/graphcool/internal-docs/blob/master/relations.md#findings
    val resolveFromBothSidesAndMerge = relationField.relation.get
      .isSameFieldSameModelRelation(project.schema) && !relationField.isList

    val query = resolveFromBothSidesAndMerge match {
      case false =>
        parentNodeIds.distinct.view.zipWithIndex.foldLeft(sql"")((a, b) =>
          a concat unionIfNotFirst(b._2) concat createQuery(b._1, modelRelationSide, fieldRelationSide))

      case true =>
        parentNodeIds.distinct.view.zipWithIndex.foldLeft(sql"")(
          (a, b) =>
            a concat unionIfNotFirst(b._2) concat createQuery(b._1, modelRelationSide, fieldRelationSide) concat sql"union all " concat createQuery(
              b._1,
              fieldRelationSide,
              modelRelationSide))
    }

    (query, resultTransform)
  }

  def countAllFromRelatedModels(project: Project,
                                relationField: Field,
                                parentNodeIds: List[String],
                                args: Option[QueryArguments]): (SQLActionBuilder, ResultTransform) = {

    val fieldTable        = relationField.relatedModel(project.schema).get.name
    val unsafeRelationId  = relationField.relation.get.id
    val modelRelationSide = relationField.relationSide.get.toString
    val fieldRelationSide = relationField.oppositeRelationSide.get.toString

    val (conditionCommand, orderByCommand, limitCommand, resultTransform) =
      extractQueryArgs(project.id, fieldTable, args, defaultOrderShortcut = Some(s"""`${project.id}`.`$unsafeRelationId`.$fieldRelationSide"""))

    def createQuery(id: String) = {
      sql"""(select '#$id', count(*) from `#${project.id}`.`#$fieldTable`
           inner join `#${project.id}`.`#$unsafeRelationId`
           on `#${project.id}`.`#$fieldTable`.id = `#${project.id}`.`#$unsafeRelationId`.#$fieldRelationSide
           where `#${project.id}`.`#$unsafeRelationId`.#$modelRelationSide = '#$id' """ concat
        prefixIfNotNone("and", conditionCommand) concat
        prefixIfNotNone("order by", orderByCommand) concat
        prefixIfNotNone("limit", limitCommand) concat sql")"
    }

    def unionIfNotFirst(index: Int): SQLActionBuilder =
      if (index == 0) {
        sql""
      } else {
        sql"union all "
      }

    val query =
      parentNodeIds.distinct.view.zipWithIndex.foldLeft(sql"")((a, b) => a concat unionIfNotFirst(b._2) concat createQuery(b._1))

    (query, resultTransform)
  }

  case class ColumnDescription(name: String, isNullable: Boolean, typeName: String, size: Option[Int])
  case class IndexDescription(name: Option[String], nonUnique: Boolean, column: Option[String])
  case class ForeignKeyDescription(name: Option[String], column: String, foreignTable: String, foreignColumn: String)
  case class TableInfo(columns: List[ColumnDescription], indexes: List[IndexDescription], foreignKeys: List[ForeignKeyDescription])

  def getTables(projectId: String): DBIOAction[Vector[String], NoStream, Read] = {
    for {
      metaTables <- MTable.getTables(cat = Some(projectId), schemaPattern = None, namePattern = None, types = None)
    } yield metaTables.map(table => table.name.name)
  }

  def getSchemas: DBIOAction[Vector[String], NoStream, Read] = {
    for {
      catalogs <- DatabaseMeta.getCatalogs
    } yield catalogs
  }

  type ResultTransform     = Function[List[DataItem], ResolverResult]
  type ResultListTransform = Function[List[ScalarListValue], ResolverResult]

}
