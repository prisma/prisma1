package cool.graph.client.database

import cool.graph.DataItem
import cool.graph.shared.models.{Field, Project}
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

      DataItem(id = rs.getString("id"), userData = userData)
    }
  }

  def selectAllFromModel(projectId: String,
                         modelName: String,
                         args: Option[QueryArguments],
                         overrideMaxNodeCount: Option[Int] = None): (SQLActionBuilder, ResultTransform) = {

    val (conditionCommand, orderByCommand, limitCommand, resultTransform) =
      extractQueryArgs(projectId, modelName, args, overrideMaxNodeCount = overrideMaxNodeCount)

    val query =
      sql"select * from `#$projectId`.`#$modelName`" concat
        prefixIfNotNone("where", conditionCommand) concat
        prefixIfNotNone("order by", orderByCommand) concat
        prefixIfNotNone("limit", limitCommand)

    (query, resultTransform)
  }

  def selectAllFromModels(projectId: String, modelName: String, args: Option[QueryArguments]): (SQLActionBuilder, ResultTransform) = {

    val (conditionCommand, orderByCommand, limitCommand, resultTransform) =
      extractQueryArgs(projectId, modelName, args)

    val query =
      sql"select * from `#$projectId`.`#$modelName`" concat
        prefixIfNotNone("where", conditionCommand) concat
        prefixIfNotNone("order by", orderByCommand) concat
        prefixIfNotNone("limit", limitCommand)

    (query, resultTransform)
  }

  def countAllFromModel(projectId: String, modelName: String, args: Option[QueryArguments]): SQLActionBuilder = {

    val (conditionCommand, orderByCommand, _, _) =
      extractQueryArgs(projectId, modelName, args)

    sql"select count(*) from `#$projectId`.`#$modelName`" concat
      prefixIfNotNone("where", conditionCommand) concat
      prefixIfNotNone("order by", orderByCommand)
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
            case None => givenArgs.extractLimitCommand(projectId, modelName)
            case Some(maxCount: Int) =>
              givenArgs.extractLimitCommand(projectId, modelName, maxCount)
          },
          givenArgs.extractResultTransform(projectId, modelName)
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

  def valueCountForScalarField(projectId: String, modelName: String, fieldName: String, value: String) = {
    sql"""SELECT COUNT(*) AS Count FROM `#$projectId`.`#$modelName`
          WHERE `#$projectId`.`#$modelName`.#$fieldName = $value"""
  }

  def existsNullByModelAndRelationField(projectId: String, modelName: String, field: Field) = {
    val relationId   = field.relation.get.id
    val relationSide = field.relationSide.get.toString
    sql"""(select EXISTS (select `id`from `#$projectId`.`#$modelName`
             where `#$projectId`.`#$modelName`.id Not IN
             (Select `#$projectId`.`#$relationId`.#$relationSide from `#$projectId`.`#$relationId`)))"""
  }

  def existsByModelAndId(projectId: String, modelName: String, id: String) = {
    sql"select exists (select `id` from `#$projectId`.`#$modelName` where `id` = '#$id')"
  }

  def existsByModel(projectId: String, modelName: String) = {
    sql"select exists (select `id` from `#$projectId`.`#$modelName`)"
  }

  def batchSelectFromModelByUnique(projectId: String, modelName: String, key: String, values: List[Any]): SQLActionBuilder = {
    sql"select * from `#$projectId`.`#$modelName` where `#$key` in (" concat combineByComma(values.map(escapeUnsafeParam)) concat sql")"
  }

  def batchSelectAllFromRelatedModel(project: Project,
                                     relationField: Field,
                                     parentNodeIds: List[String],
                                     args: Option[QueryArguments]): (SQLActionBuilder, ResultTransform) = {

    val fieldTable        = relationField.relatedModel(project).get.name
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
      .isSameFieldSameModelRelation(project) && !relationField.isList

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

    val fieldTable        = relationField.relatedModel(project).get.name
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

  def getTableInfo(projectId: String, tableName: Option[String] = None): DBIOAction[TableInfo, NoStream, Read] = {
    for {
      metaTables <- MTable
                     .getTables(cat = Some(projectId), schemaPattern = None, namePattern = tableName, types = None)
      columns     <- metaTables.head.getColumns
      indexes     <- metaTables.head.getIndexInfo(false, false)
      foreignKeys <- metaTables.head.getImportedKeys
    } yield
      TableInfo(
        columns = columns
          .map(x => ColumnDescription(name = x.name, isNullable = x.isNullable.get, typeName = x.typeName, size = x.size))
          .toList,
        indexes = indexes
          .map(x => IndexDescription(name = x.indexName, nonUnique = x.nonUnique, column = x.column))
          .toList,
        foreignKeys = foreignKeys
          .map(x => ForeignKeyDescription(name = x.fkName, column = x.fkColumn, foreignColumn = x.pkColumn, foreignTable = x.pkTable.name))
          .toList
      )
  }

  def getTables(projectId: String) = {
    for {
      metaTables <- MTable.getTables(cat = Some(projectId), schemaPattern = None, namePattern = None, types = None)
    } yield metaTables.map(table => table.name.name)
  }

  def getSchemas = {
    for {
      catalogs <- DatabaseMeta.getCatalogs
    } yield catalogs
  }

  type ResultTransform = Function[List[DataItem], ResolverResult]
}
