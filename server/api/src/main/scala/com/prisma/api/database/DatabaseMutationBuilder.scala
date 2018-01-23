package com.prisma.api.database

import com.prisma.api.database.Types.DataItemFilterCollection
import com.prisma.api.mutations.{CoolArgs, NodeSelector, ParentInfo}
import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models.{Model, Project, TypeIdentifier}
import cool.graph.cuid.Cuid
import slick.dbio.DBIOAction
import slick.jdbc.MySQLProfile.api._
import slick.sql.{SqlAction, SqlStreamingAction}

import scala.concurrent.ExecutionContext.Implicits.global

object DatabaseMutationBuilder {

  import SlickExtensions._

  val implicitlyCreatedColumns = List("id", "createdAt", "updatedAt")

  def createDataItem(projectId: String, modelName: String, args: CoolArgs): SqlStreamingAction[Vector[Int], Int, Effect]#ResultAction[Int, NoStream, Effect] = {

    val escapedKeyValueTuples = args.raw.toList.map(x => (escapeKey(x._1), escapeUnsafeParam(x._2)))
    val escapedKeys           = combineByComma(escapedKeyValueTuples.map(_._1))
    val escapedValues         = combineByComma(escapedKeyValueTuples.map(_._2))
    (sql"insert into `#$projectId`.`#$modelName` (" ++ escapedKeys ++ sql") values (" ++ escapedValues ++ sql")").asUpdate
  }

  def updateDataItems(projectId: String, model: Model, args: CoolArgs, where: DataItemFilterCollection) = {
    val updateValues = combineByComma(args.raw.map { case (k, v) => escapeKey(k) ++ sql" = " ++ escapeUnsafeParam(v) })
    val whereSql     = QueryArguments.generateFilterConditions(projectId, model.name, where)
    (sql"update `#${projectId}`.`#${model.name}`" ++ sql"set " ++ updateValues ++ prefixIfNotNone("where", whereSql)).asUpdate
  }

  def updateDataItemByUnique(projectId: String, where: NodeSelector, updateArgs: CoolArgs) = {
    val updateValues = combineByComma(updateArgs.raw.map { case (k, v) => escapeKey(k) ++ sql" = " ++ escapeUnsafeParam(v) })
    if (updateArgs.isNonEmpty) {
      (sql"update `#${projectId}`.`#${where.model.name}`" ++
        sql"set " ++ updateValues ++
        sql"where `#${where.field.name}` = ${where.fieldValue};").asUpdate
    } else {
      DBIOAction.successful(())
    }
  }

  def whereFailureTrigger(project: Project, where: NodeSelector) = {
    (sql"select case" ++
      sql"when exists" ++
      sql"(select *" ++
      sql"from `#${project.id}`.`#${where.model.name}`" ++
      sql"where `#${where.field.name}` = ${where.fieldValue})" ++
      sql"then 1" ++
      sql"else (select COLUMN_NAME" ++
      sql"from information_schema.columns" ++
      sql"where table_schema = ${project.id} AND TABLE_NAME = ${where.model.name})end;").as[Int]
  }

  def connectionFailureTrigger(project: Project, parentInfo: ParentInfo, where: NodeSelector) = {
    val childSide  = parentInfo.relation.sideOf(where.model)
    val parentSide = parentInfo.relation.sideOf(parentInfo.model)

    (sql"select case" ++
      sql"when exists" ++
      sql"(select *" ++
      sql"from `#${project.id}`.`#${parentInfo.relation.id}`" ++
      sql"where `#$childSide` = (Select `id` from `#${project.id}`.`#${where.model.name}`where `#${where.field.name}` = ${where.fieldValue})" ++
      sql"AND `#$parentSide` = (Select `id` from `#${project.id}`.`#${parentInfo.model.name}`where `#${parentInfo.where.field.name}` = ${parentInfo.where.fieldValue}))" ++
      sql"then 1" ++
      sql"else (select COLUMN_NAME" ++
      sql"from information_schema.columns" ++
      sql"where table_schema = ${project.id} AND TABLE_NAME = ${parentInfo.relation.id})end;").as[Int]
  }

  def deleteDataItems(project: Project, model: Model, where: DataItemFilterCollection) = {
    val whereSql = QueryArguments.generateFilterConditions(project.id, model.name, where)
    (sql"delete from `#${project.id}`.`#${model.name}`" ++ prefixIfNotNone("where", whereSql)).asUpdate
  }

  def deleteRelayIds(project: Project, model: Model, where: DataItemFilterCollection) = {
    val whereSql = QueryArguments.generateFilterConditions(project.id, model.name, where)
    (sql"DELETE FROM `#${project.id}`.`_RelayId`" ++
      (sql"WHERE `id` IN (" ++
        sql"SELECT `id`" ++
        sql"FROM `#${project.id}`.`#${model.name}`" ++
        prefixIfNotNone("where", whereSql) ++ sql")")).asUpdate
  }

  def createDataItemIfUniqueDoesNotExist(projectId: String, where: NodeSelector, createArgs: CoolArgs) = {
    val escapedColumns = combineByComma(createArgs.raw.keys.map(escapeKey))
    val insertValues   = combineByComma(createArgs.raw.values.map(escapeUnsafeParam))
    (sql"INSERT INTO `#${projectId}`.`#${where.model.name}` (" ++ escapedColumns ++ sql")" ++
      sql"SELECT " ++ insertValues ++
      sql"FROM DUAL" ++
      sql"where not exists (select * from `#${projectId}`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue});").asUpdate
  }

  def upsert(projectId: String, where: NodeSelector, createArgs: CoolArgs, updateArgs: CoolArgs) = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val q       = DatabaseQueryBuilder.existsFromModelsByUniques(projectId, where.model, Vector(where)).as[Boolean]
    val qInsert = createDataItemIfUniqueDoesNotExist(projectId, where, createArgs)
    val qUpdate = updateDataItemByUnique(projectId, where, updateArgs)

    for {
      exists <- q
      action <- if (exists.head) qUpdate else qInsert
    } yield action
  }

  def upsertIfInRelationWith(
      project: Project,
      parentInfo: ParentInfo,
      where: NodeSelector,
      createArgs: CoolArgs,
      updateArgs: CoolArgs
  ) = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val q       = DatabaseQueryBuilder.existsNodeIsInRelationshipWith(project, parentInfo, where).as[Boolean]
    val qInsert = createDataItem(project.id, where.model.name, createArgs)
    val qUpdate = updateDataItemByUnique(project.id, where, updateArgs)

    for {
      exists <- q
      action <- if (exists.head) qUpdate else qInsert
    } yield action
  }

  def createRelationRow(projectId: String,
                        relationTableName: String,
                        id: String,
                        a: String,
                        b: String): SqlStreamingAction[Vector[Int], Int, Effect]#ResultAction[Int, NoStream, Effect] = {

    (sql"insert into `#$projectId`.`#$relationTableName` (" ++ combineByComma(List(sql"`id`, `A`, `B`")) ++ sql") values (" ++ combineByComma(
      List(sql"$id, $a, $b")) ++ sql") on duplicate key update id=id").asUpdate
  }

  def selectIdByWhere(projectId: String, where: NodeSelector) =
    sqlu"(select id from `#$projectId`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue})"

  def createRelationRowByUniqueValueForA(projectId: String, parentInfo: ParentInfo, where: NodeSelector): SqlAction[Int, NoStream, Effect] = {
    val relationId = Cuid.createCuid()
    sqlu"""insert into `#$projectId`.`#${parentInfo.relation.id}` (`id`, `A`, `B`)
           Select '#$relationId', (select id from `#$projectId`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue}), `id`
           FROM   `#$projectId`.`#${parentInfo.model.name}` where `#${parentInfo.where.field.name}` = ${parentInfo.where.fieldValue} on duplicate key update `#$projectId`.`#${parentInfo.relation.id}`.id=`#$projectId`.`#${parentInfo.relation.id}`.id"""
  }

  def createRelationRowByUniqueValueForB(projectId: String, parentInfo: ParentInfo, where: NodeSelector): SqlAction[Int, NoStream, Effect] = {
    val relationId = Cuid.createCuid()
    sqlu"""insert into `#$projectId`.`#${parentInfo.relation.id}` (`id`, `A`, `B`)
           Select'#$relationId', (select id from `#$projectId`.`#${parentInfo.model.name}` where `#${parentInfo.where.field.name}` = ${parentInfo.where.fieldValue}), `id`
           FROM `#$projectId`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue} on duplicate key update `#$projectId`.`#${parentInfo.relation.id}`.id=`#$projectId`.`#${parentInfo.relation.id}`.id"""
  }

  def deleteRelationRowByUniqueValue(projectId: String, parentInfo: ParentInfo, where: NodeSelector): SqlAction[Int, NoStream, Effect] = {
    val parentSide = parentInfo.field.relationSide.get
    val childSide  = parentInfo.field.oppositeRelationSide.get

    sqlu"""delete from `#$projectId`.`#${parentInfo.relation.id}`
           where `#${parentSide}` = (select id from `#$projectId`.`#${parentInfo.model.name}` where `#${parentInfo.where.field.name}` = ${parentInfo.where.fieldValue}) 
           and `#${childSide}` in (
             select id
             from `#$projectId`.`#${where.model.name}`
             where `#${where.field.name}` = ${where.fieldValue}
           )
          """
  }

  def updateRelationRow(projectId: String, relationTable: String, relationSide: String, nodeId: String, values: Map[String, Any]) = {
    val escapedValues = combineByComma(values.map { case (k, v) => escapeKey(k) ++ sql" = " ++ escapeUnsafeParam(v) })

    (sql"update `#$projectId`.`#$relationTable` set" ++ escapedValues ++ sql"where `#$relationSide` = $nodeId").asUpdate
  }

  def deleteDataItemById(projectId: String, modelName: String, id: String) =
    sqlu"delete from `#$projectId`.`#$modelName` where `id` = $id"

  def deleteDataItemByUnique(projectId: String, where: NodeSelector) =
    sqlu"delete from `#$projectId`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue}"

  def deleteRelayRowByUnique(projectId: String, where: NodeSelector) =
    sqlu"delete from `#$projectId`.`_RelayId` where `id` = (select id from `#$projectId`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue})"

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
  def disableForeignKeyConstraintChecks                   = sqlu"SET FOREIGN_KEY_CHECKS=0"
  def truncateTable(projectId: String, tableName: String) = sqlu"TRUNCATE TABLE `#$projectId`.`#$tableName`"
  def enableForeignKeyConstraintChecks                    = sqlu"SET FOREIGN_KEY_CHECKS=1"

  def deleteDataItemByValues(projectId: String, modelName: String, values: Map[String, Any]) = {
    val whereClause =
      if (values.isEmpty) {
        None
      } else {
        val escapedKeys   = values.keys.map(escapeKey)
        val escapedValues = values.values.map(escapeUnsafeParam)

        val keyValueTuples = escapedKeys zip escapedValues
        combineByAnd(keyValueTuples.map({
          case (k, v) => k ++ sql" = " ++ v
        }))
      }

    val whereClauseWithWhere = if (whereClause.isEmpty) None else Some(sql"where " ++ whereClause)

    (sql"delete from `#$projectId`.`#$modelName`" ++ whereClauseWithWhere).asUpdate
  }

  def setScalarList(projectId: String, where: NodeSelector, fieldName: String, values: Vector[Any]) = {
// we can detect when it is an id and then forego the lookup in the related table
//    where.isId match {
//      case false =>
//    val escapedValueTuples = for {
//      (escapedValue, position) <- values.map(escapeUnsafeParam).zip((1 to values.length).map(_ * 1000))
//    } yield {
//      sql"(@nodeId, $position, " ++ escapedValue ++ sql")"
//    }
//    DBIO.sequence(
//      List(
//        sqlu"""set @nodeId := (select id from `#$projectId`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue})""",
//        (sql"replace into `#$projectId`.`#${where.model.name}_#${fieldName}` (`nodeId`, `position`, `value`) values " ++ combineByComma(escapedValueTuples)).asUpdate
//      ))
//      case true =>
//        val escapedValueTuples = for {
//          (escapedValue, position) <- values.map(escapeUnsafeParam).zip((1 to values.length).map(_ * 1000))
//        } yield {
//          sql"(${where.fieldValueAsString}, $position, " ++ escapedValue ++ sql")"
//        }
//        DBIO.sequence(
//          List(
//            (sql"replace into `#$projectId`.`#${where.model.name}_#${fieldName}` (`nodeId`, `position`, `value`) values " ++ combineByComma(
//              escapedValueTuples)).asUpdate))
//    }

    val escapedValueTuples = for {
      (escapedValue, position) <- values.map(escapeUnsafeParam).zip((1 to values.length).map(_ * 1000))
    } yield {
      sql"(@nodeId, $position, " ++ escapedValue ++ sql")"
    }
    DBIO.sequence(
      List(
        sqlu"""set @nodeId := (select id from `#$projectId`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue})""",
        (sql"replace into `#$projectId`.`#${where.model.name}_#${fieldName}` (`nodeId`, `position`, `value`) values " ++ combineByComma(escapedValueTuples)).asUpdate
      ))
  }

  def pushScalarList(projectId: String, modelName: String, fieldName: String, nodeId: String, values: Vector[Any]): DBIOAction[Int, NoStream, Effect] = {

    val escapedValueTuples = for {
      (escapedValue, position) <- values.map(escapeUnsafeParam).zip((1 to values.length).map(_ * 1000))
    } yield {
      sql"($nodeId, @baseline + $position, " ++ escapedValue ++ sql")"
    }

    DBIO
      .sequence(
        List(
          sqlu"""set @baseline := ifnull((select max(position) from `#$projectId`.`#${modelName}_#${fieldName}` where nodeId = $nodeId), 0) + 1000""",
          (sql"insert into `#$projectId`.`#${modelName}_#${fieldName}` (`nodeId`, `position`, `value`) values " ++ combineByComma(escapedValueTuples)).asUpdate
        ))
      .map(_.last)
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
      case TypeIdentifier.Relation  => sys.error("Relation is not a scalar type. Are you trying to create a db column for a relation?")
    }
  }
}
