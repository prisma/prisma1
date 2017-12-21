package cool.graph.api.database

import cool.graph.api.database.Types.DataItemFilterCollection
import cool.graph.api.mutations.{CoolArgs, NodeSelector}
import cool.graph.cuid.Cuid
import cool.graph.gc_values._
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.RelationSide.RelationSide
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import cool.graph.shared.models.{Model, Project, Relation, TypeIdentifier}
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._
import slick.dbio.DBIOAction
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.{PositionedParameters, SetParameter}
import slick.sql.{SqlAction, SqlStreamingAction}

object DatabaseMutationBuilder {

  import SlickExtensions._

  val implicitlyCreatedColumns = List("id", "createdAt", "updatedAt")

  def createDataItem(project: Project, model: Model, args: CoolArgs): SqlStreamingAction[Vector[Int], Int, Effect]#ResultAction[Int, NoStream, Effect] = {
    createDataItem(project.id, model.name, args.raw)
  }

  def createDataItem(projectId: String,
                     modelName: String,
                     values: Map[String, Any]): SqlStreamingAction[Vector[Int], Int, Effect]#ResultAction[Int, NoStream, Effect] = {

    val escapedKeyValueTuples = values.toList.map(x => (escapeKey(x._1), escapeUnsafeParam(x._2)))
    val escapedKeys           = combineByComma(escapedKeyValueTuples.map(_._1))
    val escapedValues         = combineByComma(escapedKeyValueTuples.map(_._2))

    // Concat query as sql, but then convert it to Update, since is an insert query.
    (sql"insert into `#$projectId`.`#$modelName` (" concat escapedKeys concat sql") values (" concat escapedValues concat sql")").asUpdate
  }

  def updateDataItem(projectId: String, modelName: String, id: String, values: Map[String, Any]) = {
    val escapedValues = combineByComma(values.map { case (k, v) => escapeKey(k) concat sql" = " concat escapeUnsafeParam(v) })

    (sql"update `#$projectId`.`#$modelName` set" concat escapedValues concat sql"where id = $id").asUpdate
  }

  def updateDataItems(project: Project, model: Model, args: CoolArgs, where: DataItemFilterCollection) = {
    val updateValues = combineByComma(args.raw.map { case (k, v) => escapeKey(k) ++ sql" = " ++ escapeUnsafeParam(v) })
    val whereSql     = QueryArguments.generateFilterConditions(project.id, model.name, where)
    (sql"update `#${project.id}`.`#${model.name}`" ++
      sql"set " ++ updateValues ++
      prefixIfNotNone("where", whereSql)).asUpdate
  }

  def updateDataItemByUnique(project: Project, model: Model, updateArgs: CoolArgs, where: NodeSelector) = {
    val updateValues = combineByComma(updateArgs.raw.map { case (k, v) => escapeKey(k) ++ sql" = " ++ escapeUnsafeParam(v) })
    (sql"update `#${project.id}`.`#${model.name}`" ++
      sql"set " ++ updateValues ++
      sql"where #${where.fieldName} = ${where.fieldValue};").asUpdate
  }

  def deleteDataItems(project: Project, model: Model, where: DataItemFilterCollection) = {
    val whereSql = QueryArguments.generateFilterConditions(project.id, model.name, where)
    (sql"delete from `#${project.id}`.`#${model.name}`" ++ prefixIfNotNone("where", whereSql)).asUpdate
  }

  def createDataItemIfUniqueDoesNotExist(project: Project, model: Model, createArgs: CoolArgs, where: NodeSelector) = {
    val escapedColumns = combineByComma(createArgs.raw.keys.map(escapeKey))
    val insertValues   = combineByComma(createArgs.raw.values.map(escapeUnsafeParam))
    (sql"INSERT INTO `#${project.id}`.`#${model.name}` (" ++ escapedColumns ++ sql")" ++
      sql"SELECT " ++ insertValues ++
      sql"FROM DUAL" ++
      sql"where not exists (select * from `#${project.id}`.`#${model.name}` where #${where.fieldName} = ${where.fieldValue});").asUpdate
  }

  def upsert(project: Project, model: Model, createArgs: CoolArgs, updateArgs: CoolArgs, where: NodeSelector) = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val q       = DatabaseQueryBuilder.existsFromModelsByUniques(project, model, Vector(where)).as[Boolean]
    val qInsert = createDataItemIfUniqueDoesNotExist(project, model, createArgs, where)
    val qUpdate = updateDataItemByUnique(project, model, updateArgs, where)

    val actions = for {
      exists <- q
      action <- if (exists.head) qUpdate else qInsert
    } yield action

    actions.transactionally
  }

  def upsertIfInRelationWith(
      project: Project,
      model: Model,
      createArgs: CoolArgs,
      updateArgs: CoolArgs,
      where: NodeSelector,
      relation: Relation,
      target: Id
  ) = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val q       = DatabaseQueryBuilder.existsNodeIsInRelationshipWith(project, model, where, relation, target).as[Boolean]
    val qInsert = createDataItem(project, model, createArgs)
    val qUpdate = updateDataItemByUnique(project, model, updateArgs, where)

    val actions = for {
      exists <- q
      action <- if (exists.head) qUpdate else qInsert
    } yield action

    actions.transactionally
  }

  case class MirrorFieldDbValues(relationColumnName: String, modelColumnName: String, modelTableName: String, modelId: String)

  def createRelationRow(projectId: String,
                        relationTableName: String,
                        id: String,
                        a: String,
                        b: String,
                        fieldMirrors: List[MirrorFieldDbValues]): SqlStreamingAction[Vector[Int], Int, Effect]#ResultAction[Int, NoStream, Effect] = {

    val fieldMirrorColumns = fieldMirrors.map(_.relationColumnName).map(escapeKey)

    val fieldMirrorValues =
      fieldMirrors.map(mirror => sql"(SELECT `#${mirror.modelColumnName}` FROM `#$projectId`.`#${mirror.modelTableName}` WHERE id = ${mirror.modelId})")

    // Concat query as sql, but then convert it to Update, since is an insert query.
    (sql"insert into `#$projectId`.`#$relationTableName` (" concat combineByComma(List(sql"`id`, `A`, `B`") ++ fieldMirrorColumns) concat sql") values (" concat combineByComma(
      List(sql"$id, $a, $b") ++ fieldMirrorValues) concat sql") on duplicate key update id=id").asUpdate
  }

  def createRelationRowByUniqueValueForA(projectId: String, relationTableName: String, b: String, where: NodeSelector): SqlAction[Int, NoStream, Effect] = {
    val relationId = Cuid.createCuid()
    sqlu"""insert into `#$projectId`.`#$relationTableName` (`id`, `A`, `B`)
           select '#$relationId', id, '#$b' from `#$projectId`.`#${where.model.name}`
           where #${where.fieldName} = ${where.fieldValue}
          """
  }

  def createRelationRowByUniqueValueForB(projectId: String, relationTableName: String, a: String, where: NodeSelector): SqlAction[Int, NoStream, Effect] = {
    val relationId = Cuid.createCuid()
    sqlu"""insert into `#$projectId`.`#$relationTableName` (`id`, `A`, `B`)
           select '#$relationId', '#$a', id from `#$projectId`.`#${where.model.name}`
           where #${where.fieldName} = ${where.fieldValue}
          """
  }

  def deleteRelationRowByUniqueValueForA(projectId: String, relationTableName: String, b: String, where: NodeSelector): SqlAction[Int, NoStream, Effect] = {
    sqlu"""delete from `#$projectId`.`#$relationTableName`
           where `B` = '#$b' and `A` in (
             select id
             from `#$projectId`.`#${where.model.name}`
             where #${where.fieldName} = ${where.fieldValue}
           )
          """
  }

  def deleteRelationRowByUniqueValueForB(projectId: String, relationTableName: String, a: String, where: NodeSelector): SqlAction[Int, NoStream, Effect] = {
    sqlu"""delete from `#$projectId`.`#$relationTableName`
           where `A` = '#$a' and `B` in (
             select id
             from `#$projectId`.`#${where.model.name}`
             where #${where.fieldName} = ${where.fieldValue}
           )
          """
  }

  def deleteDataItemByUniqueValueForAIfInRelationWithGivenB(projectId: String, relationTableName: String, b: String, where: NodeSelector) = {
    sqlu"""delete from `#$projectId`.`#${where.model.name}`
           where #${where.fieldName} = ${where.fieldValue} and id in (
             select `A`
             from `#$projectId`.`#$relationTableName`
             where `B` = '#$b'
           )
           """
  }

  def deleteDataItemByUniqueValueForBIfInRelationWithGivenA(projectId: String, relationTableName: String, a: String, where: NodeSelector) = {
    sqlu"""delete from `#$projectId`.`#${where.model.name}`
           where #${where.fieldName} = ${where.fieldValue} and id in (
             select `B`
             from `#$projectId`.`#$relationTableName`
             where `A` = '#$a'
           )
           """
  }

  def updateDataItemByUniqueValueForAIfInRelationWithGivenB(projectId: String,
                                                            relationTableName: String,
                                                            b: String,
                                                            where: NodeSelector,
                                                            values: Map[String, Any]) = {
    val escapedValues = combineByComma(values.map { case (k, v) => escapeKey(k) concat sql" = " concat escapeUnsafeParam(v) })
    (sql"""update `#$projectId`.`#${where.model.name}`""" concat
      sql"""set""" concat escapedValues concat
      sql"""where #${where.fieldName} = ${where.fieldValue} and id in (
             select `A`
             from `#$projectId`.`#$relationTableName`
             where `B` = '#$b'
           )
        """).asUpdate
  }

  def updateDataItemByUniqueValueForBIfInRelationWithGivenA(projectId: String,
                                                            relationTableName: String,
                                                            a: String,
                                                            where: NodeSelector,
                                                            values: Map[String, Any]) = {
    val escapedValues = combineByComma(values.map { case (k, v) => escapeKey(k) concat sql" = " concat escapeUnsafeParam(v) })
    (sql"""update `#$projectId`.`#${where.model.name}`""" concat
      sql"""set""" concat escapedValues concat
      sql"""where #${where.fieldName} = ${where.fieldValue} and id in (
             select `B`
             from `#$projectId`.`#$relationTableName`
             where `A` = '#$a'
           )
        """).asUpdate
  }

  def updateDataItemListValue(projectId: String, modelName: String, id: String, values: Map[String, Vector[Any]]) = {
    val (fieldName, commaSeparatedValues) = values.map { case (k, v) => (k, escapeUnsafeParamListValue(v)) }.head

    (sql"update `#$projectId`.`#$modelName`" concat
      sql"set`#$fieldName` = CASE WHEN `#$fieldName` like '[]'" concat
      sql"THEN Concat(LEFT(`#$fieldName`,LENGTH(`#$fieldName`)-1)," concat commaSeparatedValues concat sql",']')" concat
      sql"ELSE Concat(LEFT(`#$fieldName`,LENGTH(`#$fieldName`)-1),','," concat commaSeparatedValues concat sql",']') END " concat
      sql"where id = $id").asUpdate
  }

  def updateRelationRow(projectId: String, relationTable: String, relationSide: String, nodeId: String, values: Map[String, Any]) = {
    val escapedValues = combineByComma(values.map { case (k, v) => escapeKey(k) concat sql" = " concat escapeUnsafeParam(v) })

    (sql"update `#$projectId`.`#$relationTable` set" concat escapedValues concat sql"where `#$relationSide` = $nodeId").asUpdate
  }

  def populateNullRowsForColumn(projectId: String, modelName: String, fieldName: String, value: Any) = {
    val escapedValues = escapeKey(fieldName) concat sql" = " concat escapeUnsafeParam(value)

    (sql"update `#$projectId`.`#$modelName` set" concat escapedValues concat sql"where `#$projectId`.`#$modelName`.`#$fieldName` IS NULL").asUpdate
  }

  def overwriteInvalidEnumForColumnWithMigrationValue(projectId: String, modelName: String, fieldName: String, oldValue: String, migrationValue: String) = {
    val escapedValues      = escapeKey(fieldName) concat sql" = " concat escapeUnsafeParam(migrationValue)
    val escapedWhereClause = escapeKey(fieldName) concat sql" = " concat escapeUnsafeParam(oldValue)

    (sql"update `#$projectId`.`#$modelName` set" concat escapedValues concat sql"where" concat escapedWhereClause).asUpdate
  }

  def overwriteAllRowsForColumn(projectId: String, modelName: String, fieldName: String, value: Any) = {
    val escapedValues = escapeKey(fieldName) concat sql" = " concat escapeUnsafeParam(value)

    (sql"update `#$projectId`.`#$modelName` set" concat escapedValues).asUpdate
  }

  def deleteDataItemById(projectId: String, modelName: String, id: String) =
    sqlu"delete from `#$projectId`.`#$modelName` where id = $id"

  def deleteRelationRowById(projectId: String, relationId: String, id: String) =
    sqlu"delete from `#$projectId`.`#$relationId` where A = $id or B = $id"

  def deleteRelationRowBySideAndId(projectId: String, relationId: String, relationSide: RelationSide, id: String) = {
    sqlu"delete from `#$projectId`.`#$relationId` where `#${relationSide.toString}` = $id"
  }

  def deleteRelationRowByToAndFromSideAndId(projectId: String,
                                            relationId: String,
                                            aRelationSide: RelationSide,
                                            aId: String,
                                            bRelationSide: RelationSide,
                                            bId: String) = {
    sqlu"delete from `#$projectId`.`#$relationId` where `#${aRelationSide.toString}` = $aId and `#${bRelationSide.toString}` = $bId"
  }

  def deleteAllDataItems(projectId: String, modelName: String) =
    sqlu"delete from `#$projectId`.`#$modelName`"

  //only use transactionally in this order
  def disableForeignKeyConstraintChecks = sqlu"SET FOREIGN_KEY_CHECKS=0"
  def truncateTable(projectId: String, tableName: String) =
    sqlu"TRUNCATE TABLE `#$projectId`.`#$tableName`"
  def enableForeignKeyConstraintChecks = sqlu"SET FOREIGN_KEY_CHECKS=1"

  def deleteDataItemByValues(projectId: String, modelName: String, values: Map[String, Any]) = {
    val whereClause =
      if (values.isEmpty) {
        None
      } else {
        val escapedKeys   = values.keys.map(escapeKey)
        val escapedValues = values.values.map(escapeUnsafeParam)

        val keyValueTuples = escapedKeys zip escapedValues
        combineByAnd(keyValueTuples.map({
          case (k, v) => k concat sql" = " concat v
        }))
      }

    val whereClauseWithWhere = if (whereClause.isEmpty) None else Some(sql"where " concat whereClause)

    (sql"delete from `#$projectId`.`#$modelName`" concat whereClauseWithWhere).asUpdate
  }

  def createClientDatabaseForProject(projectId: String) = {
    val idCharset = charsetTypeForScalarTypeIdentifier(isList = false, TypeIdentifier.GraphQLID)

    DBIO.seq(
      sqlu"""CREATE SCHEMA `#$projectId` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; """,
      sqlu"""CREATE TABLE `#$projectId`.`_RelayId` (`id` CHAR(25) #$idCharset NOT NULL, `modelId` CHAR(25) #$idCharset NOT NULL, PRIMARY KEY (`id`), UNIQUE INDEX `id_UNIQUE` (`id` ASC)) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"""
    )
  }

  def copyTableData(sourceProjectId: String, sourceTableName: String, columns: List[String], targetProjectId: String, targetTableName: String) = {
    val columnString = combineByComma(columns.map(c => escapeKey(c)))
    (sql"INSERT INTO `#$targetProjectId`.`#$targetTableName` (" concat columnString concat sql") SELECT " concat columnString concat sql" FROM `#$sourceProjectId`.`#$sourceTableName`").asUpdate
  }

  def dropDatabaseIfExists(database: String) =
    sqlu"DROP DATABASE IF EXISTS `#$database`"

  def createTable(projectId: String, name: String) = {
    val idCharset = charsetTypeForScalarTypeIdentifier(isList = false, TypeIdentifier.GraphQLID)

    sqlu"""CREATE TABLE `#$projectId`.`#$name`
    (`id` CHAR(25) #$idCharset NOT NULL,
    `createdAt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `id_UNIQUE` (`id` ASC))
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"""
  }

  def dangerouslyTruncateTable(tableNames: Vector[String]): DBIOAction[Unit, NoStream, Effect] = {
    DBIO.seq(
      List(sqlu"""SET FOREIGN_KEY_CHECKS=0""") ++
        tableNames.map(name => sqlu"TRUNCATE TABLE `#$name`") ++
        List(sqlu"""SET FOREIGN_KEY_CHECKS=1"""): _*
    )
  }

  def renameTable(projectId: String, name: String, newName: String) =
    sqlu"""RENAME TABLE `#$projectId`.`#$name` TO `#$projectId`.`#$newName`;"""

  def createRelationTable(projectId: String, tableName: String, aTableName: String, bTableName: String) = {
    val idCharset = charsetTypeForScalarTypeIdentifier(isList = false, TypeIdentifier.GraphQLID)

    sqlu"""CREATE TABLE `#$projectId`.`#$tableName` (`id` CHAR(25) #$idCharset NOT NULL,
           PRIMARY KEY (`id`), UNIQUE INDEX `id_UNIQUE` (`id` ASC),
    `A` CHAR(25) #$idCharset NOT NULL, INDEX `A` (`A` ASC),
    `B` CHAR(25) #$idCharset NOT NULL, INDEX `B` (`B` ASC),
    UNIQUE INDEX `AB_unique` (`A` ASC, `B` ASC),
    FOREIGN KEY (A) REFERENCES `#$projectId`.`#$aTableName`(id) ON DELETE CASCADE,
    FOREIGN KEY (B) REFERENCES `#$projectId`.`#$bTableName`(id) ON DELETE CASCADE)
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"""
  }

  def dropTable(projectId: String, tableName: String) =
    sqlu"DROP TABLE `#$projectId`.`#$tableName`"

  def createColumn(projectId: String,
                   tableName: String,
                   columnName: String,
                   isRequired: Boolean,
                   isUnique: Boolean,
                   isList: Boolean,
                   typeIdentifier: TypeIdentifier.TypeIdentifier) = {

    val sqlType       = sqlTypeForScalarTypeIdentifier(isList, typeIdentifier)
    val charsetString = charsetTypeForScalarTypeIdentifier(isList, typeIdentifier)
    val nullString    = if (isRequired) "NOT NULL" else "NULL"
    val uniqueString =
      if (isUnique) {
        val indexSize = sqlType match {
          case "text" | "mediumtext" => "(191)"
          case _                     => ""
        }

        s", ADD UNIQUE INDEX `${columnName}_UNIQUE` (`$columnName`$indexSize ASC)"
      } else { "" }

    sqlu"""ALTER TABLE `#$projectId`.`#$tableName` ADD COLUMN `#$columnName`
         #$sqlType #$charsetString #$nullString #$uniqueString, ALGORITHM = INPLACE"""
  }

  def updateColumn(projectId: String,
                   tableName: String,
                   oldColumnName: String,
                   newColumnName: String,
                   newIsRequired: Boolean,
                   newIsUnique: Boolean,
                   newIsList: Boolean,
                   newTypeIdentifier: TypeIdentifier) = {
    val nulls   = if (newIsRequired) { "NOT NULL" } else { "NULL" }
    val sqlType = sqlTypeForScalarTypeIdentifier(newIsList, newTypeIdentifier)

    sqlu"ALTER TABLE `#$projectId`.`#$tableName` CHANGE COLUMN `#$oldColumnName` `#$newColumnName` #$sqlType #$nulls"
  }

  def addUniqueConstraint(projectId: String, tableName: String, columnName: String, typeIdentifier: TypeIdentifier, isList: Boolean) = {
    val sqlType = sqlTypeForScalarTypeIdentifier(isList = isList, typeIdentifier = typeIdentifier)

    val indexSize = sqlType match {
      case "text" | "mediumtext" => "(191)"
      case _                     => ""
    }

    sqlu"ALTER TABLE  `#$projectId`.`#$tableName` ADD UNIQUE INDEX `#${columnName}_UNIQUE` (`#$columnName`#$indexSize ASC)"
  }

  def removeUniqueConstraint(projectId: String, tableName: String, columnName: String) = {
    sqlu"ALTER TABLE  `#$projectId`.`#$tableName` DROP INDEX `#${columnName}_UNIQUE`"
  }

  def deleteColumn(projectId: String, tableName: String, columnName: String) = {
    sqlu"ALTER TABLE `#$projectId`.`#$tableName` DROP COLUMN `#$columnName`, ALGORITHM = INPLACE"
  }

  def populateRelationFieldMirror(projectId: String, relationTable: String, modelTable: String, mirrorColumn: String, column: String, relationSide: String) = {
    sqlu"UPDATE `#$projectId`.`#$relationTable` R, `#$projectId`.`#$modelTable` M SET R.`#$mirrorColumn` = M.`#$column` WHERE R.`#$relationSide` = M.id;"
  }

  // note: utf8mb4 requires up to 4 bytes per character and includes full utf8 support, including emoticons
  // utf8 requires up to 3 bytes per character and does not have full utf8 support.
  // mysql indexes have a max size of 767 bytes or 191 utf8mb4 characters.
  // We limit enums to 191, and create text indexes over the first 191 characters of the string, but
  // allow the actual content to be much larger.
  // Key columns are utf8_general_ci as this collation is ~10% faster when sorting and requires less memory
  def sqlTypeForScalarTypeIdentifier(isList: Boolean, typeIdentifier: TypeIdentifier): String = {
    if (isList) return "mediumtext"

    typeIdentifier match {
      case TypeIdentifier.String    => "mediumtext"
      case TypeIdentifier.Boolean   => "boolean"
      case TypeIdentifier.Int       => "int"
      case TypeIdentifier.Float     => "Decimal(65,30)"
      case TypeIdentifier.GraphQLID => "char(25)"
      case TypeIdentifier.Enum      => "varchar(191)"
      case TypeIdentifier.Json      => "mediumtext"
      case TypeIdentifier.DateTime  => "datetime(3)"
      case TypeIdentifier.Relation =>
        sys.error("Relation is not a scalar type. Are you trying to create a db column for a relation?")
    }
  }

  def charsetTypeForScalarTypeIdentifier(isList: Boolean, typeIdentifier: TypeIdentifier): String = {
    if (isList) return "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"

    typeIdentifier match {
      case TypeIdentifier.String    => "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
      case TypeIdentifier.Boolean   => ""
      case TypeIdentifier.Int       => ""
      case TypeIdentifier.Float     => ""
      case TypeIdentifier.GraphQLID => "CHARACTER SET utf8 COLLATE utf8_general_ci"
      case TypeIdentifier.Enum      => "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
      case TypeIdentifier.Json      => "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
      case TypeIdentifier.DateTime  => ""
    }
  }

  def createTableForModel(projectId: String, model: Model) = {
    DBIO.seq(
      DBIO.seq(createTable(projectId, model.name)),
      DBIO.seq(
        model.scalarFields
          .filter(f => !DatabaseMutationBuilder.implicitlyCreatedColumns.contains(f.name))
          .map { (field) =>
            createColumn(
              projectId = projectId,
              tableName = model.name,
              columnName = field.name,
              isRequired = field.isRequired,
              isUnique = field.isUnique,
              isList = field.isList,
              typeIdentifier = field.typeIdentifier
            )
          }: _*)
    )
  }
}
