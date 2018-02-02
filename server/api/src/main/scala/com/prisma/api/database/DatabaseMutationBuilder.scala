package com.prisma.api.database

import com.prisma.api.database.CascadingDeletes.Path
import com.prisma.api.database.Types.DataItemFilterCollection
import com.prisma.api.mutations.{CoolArgs, NodeSelector, ParentInfo}
import com.prisma.api.schema.GeneralError
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models.{Model, Project, Relation, TypeIdentifier}
import cool.graph.cuid.Cuid
import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.SQLActionBuilder
import slick.sql.{SqlAction, SqlStreamingAction}

import scala.concurrent.ExecutionContext.Implicits.global

object DatabaseMutationBuilder {

  import SlickExtensions._

  val implicitlyCreatedColumns = List("id", "createdAt", "updatedAt")

  //CREATE

  def createDataItem(projectId: String, modelName: String, args: CoolArgs): SqlStreamingAction[Vector[Int], Int, Effect]#ResultAction[Int, NoStream, Effect] = {

    val escapedKeyValueTuples = args.raw.toList.map(x => (escapeKey(x._1), escapeUnsafeParam(x._2)))
    val escapedKeys           = combineByComma(escapedKeyValueTuples.map(_._1))
    val escapedValues         = combineByComma(escapedKeyValueTuples.map(_._2))
    (sql"insert into `#$projectId`.`#$modelName` (" ++ escapedKeys ++ sql") values (" ++ escapedValues ++ sql")").asUpdate
  }

  def createDataItemIfUniqueDoesNotExist(projectId: String,
                                         where: NodeSelector,
                                         createArgs: CoolArgs): SqlStreamingAction[Vector[Int], Int, Effect]#ResultAction[Int, NoStream, Effect] = {
    val escapedColumns = combineByComma(createArgs.raw.keys.map(escapeKey))
    val insertValues   = combineByComma(createArgs.raw.values.map(escapeUnsafeParam))
    (sql"INSERT INTO `#${projectId}`.`#${where.model.name}` (" ++ escapedColumns ++ sql")" ++
      sql"SELECT " ++ insertValues ++
      sql"FROM DUAL" ++
      sql"where not exists (select `id` from `#${projectId}`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue} limit 1);").asUpdate
  }

  def createRelationRow(projectId: String,
                        relationTableName: String,
                        id: String,
                        a: String,
                        b: String): SqlStreamingAction[Vector[Int], Int, Effect]#ResultAction[Int, NoStream, Effect] = {

    (sql"insert into `#$projectId`.`#$relationTableName` (" ++ combineByComma(List(sql"`id`, `A`, `B`")) ++ sql") values (" ++ combineByComma(
      List(sql"$id, $a, $b")) ++ sql") on duplicate key update id=id").asUpdate
  }

  def createRelationRowByUniqueValueForChild(projectId: String, parentInfo: ParentInfo, where: NodeSelector): SqlAction[Int, NoStream, Effect] = {
    val parentSide = parentInfo.field.relationSide.get
    val childSide  = parentInfo.field.oppositeRelationSide.get
    val relationId = Cuid.createCuid()
    sqlu"""insert into `#$projectId`.`#${parentInfo.relation.id}` (`id`, `#$parentSide`, `#$childSide`)
           Select '#$relationId', (select id from `#$projectId`.`#${parentInfo.model.name}` where `#${parentInfo.where.field.name}` = ${parentInfo.where.fieldValue}), `id`
           FROM `#$projectId`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue} on duplicate key update `#$projectId`.`#${parentInfo.relation.id}`.id=`#$projectId`.`#${parentInfo.relation.id}`.id"""
  }

  //UPDATE

  def updateDataItems(projectId: String, model: Model, args: CoolArgs, whereFilter: DataItemFilterCollection) = {
    val updateValues = combineByComma(args.raw.map { case (k, v) => escapeKey(k) ++ sql" = " ++ escapeUnsafeParam(v) })
    val whereSql     = QueryArguments.generateFilterConditions(projectId, model.name, whereFilter)
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

  //UPSERT

  def upsert(projectId: String,
             where: NodeSelector,
             createArgs: CoolArgs,
             updateArgs: CoolArgs,
             create: Vector[DBIOAction[Any, NoStream, Effect]],
             update: Vector[DBIOAction[Any, NoStream, Effect]]) = {

    val q       = DatabaseQueryBuilder.existsByWhere(projectId, where).as[Boolean]
    val qInsert = DBIOAction.seq(createDataItemIfUniqueDoesNotExist(projectId, where, createArgs), DBIOAction.seq(create: _*))
    val qUpdate = DBIOAction.seq(updateDataItemByUnique(projectId, where, updateArgs), DBIOAction.seq(update: _*))

    ifThenElse(q, qUpdate, qInsert)
  }

  def upsertIfInRelationWith(
      project: Project,
      parentInfo: ParentInfo,
      where: NodeSelector,
      createWhere: NodeSelector,
      createArgs: CoolArgs,
      updateArgs: CoolArgs,
      create: Vector[DBIOAction[Any, NoStream, Effect]],
      update: Vector[DBIOAction[Any, NoStream, Effect]]
  ) = {

    val q       = DatabaseQueryBuilder.existsNodeIsInRelationshipWith(project, parentInfo, where).as[Boolean]
    val qInsert = DBIOAction.seq(createDataItem(project.id, where.model.name, createArgs), DBIOAction.seq(create: _*))
    val qUpdate = DBIOAction.seq(updateDataItemByUnique(project.id, where, updateArgs), DBIOAction.seq(update: _*))

    ifThenElse(q, qUpdate, qInsert)
  }

  //DELETE

  def deleteDataItems(project: Project, model: Model, whereFilter: DataItemFilterCollection) = {
    val whereSql = QueryArguments.generateFilterConditions(project.id, model.name, whereFilter)
    (sql"delete from `#${project.id}`.`#${model.name}`" ++ prefixIfNotNone("where", whereSql)).asUpdate
  }

  def deleteDataItemByUnique(projectId: String, where: NodeSelector) =
    sqlu"delete from `#$projectId`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue}"

  def deleteRelayIds(project: Project, model: Model, whereFilter: DataItemFilterCollection) = {
    val whereSql = QueryArguments.generateFilterConditions(project.id, model.name, whereFilter)
    (sql"DELETE FROM `#${project.id}`.`_RelayId`" ++
      (sql"WHERE `id` IN (" ++
        sql"SELECT `id`" ++
        sql"FROM `#${project.id}`.`#${model.name}`" ++
        prefixIfNotNone("where", whereSql) ++ sql")")).asUpdate
  }

  def deleteRelayRowByUnique(projectId: String, where: NodeSelector) =
    sqlu"delete from `#$projectId`.`_RelayId` where `id` = (select id from `#$projectId`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue})"

  def deleteRelationRowByParent(projectId: String, parentInfo: ParentInfo) = {

    (sql"delete from `#$projectId`.`#${parentInfo.relation.id}` " ++
      sql"where `#${parentInfo.field.relationSide.get}` = (select id from `#$projectId`.`#${parentInfo.where.model.name}` " ++
      sql"where `#${parentInfo.where.field.name}` = ${parentInfo.where.fieldValue})").asUpdate
  }

  def deleteRelationRowByChild(projectId: String, relation: Relation, where: NodeSelector) = {

    (sql"delete from `#$projectId`.`#${relation.id}` " ++
      sql"where `#${relation.sideOf(where.model)}` = (select id from `#$projectId`.`#${where.model.name}` " ++
      sql"where `#${where.field.name}` = ${where.fieldValue})").asUpdate
  }

  def deleteRelationRowByParentAndChild(projectId: String, parentInfo: ParentInfo, where: NodeSelector) = {

    (sql"delete from `#$projectId`.`#${parentInfo.relation.id}` " ++
      sql"where " ++
      sql"`#${parentInfo.field.oppositeRelationSide.get}` = (select id from `#$projectId`.`#${where.model.name}` " ++
      sql"where `#${where.field.name}` = ${where.fieldValue})" ++
      sql" AND `#${parentInfo.field.relationSide.get}` = (select id from `#$projectId`.`#${parentInfo.where.model.name}` " ++
      sql"where `#${parentInfo.where.field.name}` = ${parentInfo.where.fieldValue})").asUpdate
  }

  def deleteRelationRowByPath(projectId: String, relation: Relation, path: Path) = {
    val childModel: Model           = path.mwrs.reverse.head.child
    val pathQuery: SQLActionBuilder = ???

    (sql"delete from `#$projectId`.`#${relation.id}` " ++
      sql"where `#${relation.sideOf(childModel)}` in ( " ++ pathQuery ++ sql"").asUpdate
  }

  //SCALAR LISTS

  def setScalarList(projectId: String, where: NodeSelector, fieldName: String, values: Vector[Any]) = {
    // todo we could save a query on ID NodeSelectors
    val escapedValueTuples = for {
      (escapedValue, position) <- values.map(escapeUnsafeParam).zip((1 to values.length).map(_ * 1000))
    } yield {
      sql"(@nodeId, $position, " ++ escapedValue ++ sql")"
    }

    DBIO.seq(
      sqlu"""set @nodeId := (select id from `#$projectId`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue})""",
      sqlu"""delete from `#$projectId`.`#${where.model.name}_#${fieldName}` where nodeId = @nodeId""",
      (sql"insert into `#$projectId`.`#${where.model.name}_#${fieldName}` (`nodeId`, `position`, `value`) values " concat combineByComma(escapedValueTuples)).asUpdate
    )
  }

  def setScalarListToEmpty(projectId: String, where: NodeSelector, fieldName: String) = {
    sql"DELETE FROM `#$projectId`.`#${where.model.name}_#${fieldName}` WHERE `nodeId` = (select id from `#$projectId`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue})".asUpdate
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

  //TRANSACTIONAL FAILURE TRIGGERS

  def whereFailureTrigger(project: Project, where: NodeSelector) = {
    val table = where.model.name
    val query = sql"select *" ++
      sql"from `#${project.id}`.`#${where.model.name}`" ++
      sql"where `#${where.field.name}` = ${where.fieldValue}"

    triggerFailureWhen(project, query, table)
  }

  def connectionFailureTrigger(project: Project, parentInfo: ParentInfo, where: NodeSelector) = {
    val childSide  = parentInfo.relation.sideOf(where.model)
    val parentSide = parentInfo.relation.sideOf(parentInfo.model)
    val table      = parentInfo.relation.id
    val query = sql"select *" ++
      sql"from `#${project.id}`.`#$table`" ++
      sql"where `#$childSide` = (Select `id` from `#${project.id}`.`#${where.model.name}`where `#${where.field.name}` = ${where.fieldValue})" ++
      sql"AND `#$parentSide` = (Select `id` from `#${project.id}`.`#${parentInfo.model.name}`where `#${parentInfo.where.field.name}` = ${parentInfo.where.fieldValue})"

    triggerFailureWhen(project, query, table)
  }

  def oldParentFailureTriggerForRequiredRelations(project: Project, relation: Relation, where: NodeSelector) = {
    val childSide = relation.sideOf(where.model)
    val table     = relation.id
    val query = sql"select *" ++
      sql"from `#${project.id}`.`#$table`" ++
      sql"where `#$childSide` = (Select `id` from `#${project.id}`.`#${where.model.name}`where `#${where.field.name}` = ${where.fieldValue})"

    triggerFailureWhenNot(project, query, table)
  }

  def oldChildFailureTriggerForRequiredRelations(project: Project, parentInfo: ParentInfo) = {
    val parentSide = parentInfo.relation.sideOf(parentInfo.model)
    val table      = parentInfo.relation.id
    val query = sql"select *" ++
      sql"from `#${project.id}`.`#$table`" ++
      sql"where `#$parentSide` = (Select `id` " ++
      sql"from `#${project.id}`.`#${parentInfo.where.model.name}` " ++
      sql"where `#${parentInfo.where.field.name}` = ${parentInfo.where.fieldValue})"

    triggerFailureWhenNot(project, query, table)
  }

  def oldParentFailureTriggerForRequiredRelationsByPath(project: Project, relation: Relation, path: Path) = {
    val childSide                   = relation.sideOf(path.mwrs.reverse.head.child)
    val table                       = relation.id
    val pathQuery: SQLActionBuilder = ???

    val query = sql"select *" ++
      sql"from `#${project.id}`.`#$table`" ++
      sql"where `#$childSide` IN ( " ++ pathQuery ++ sql" )"

    triggerFailureWhenNot(project, query, table)
  }

  // Control Flow

  def ifThenElse(condition: SqlStreamingAction[Vector[Boolean], Boolean, Effect],
                 thenMutactions: DBIOAction[Unit, NoStream, Effect],
                 elseMutactions: DBIOAction[Unit, NoStream, Effect]) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    for {
      exists <- condition
      action <- if (exists.head) thenMutactions else elseMutactions
    } yield action
  }

  def ifThenElseError(condition: SqlStreamingAction[Vector[Boolean], Boolean, Effect],
                      thenMutactions: DBIOAction[Unit, NoStream, Effect],
                      elseError: GeneralError) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    for {
      exists <- condition
      action <- if (exists.head) thenMutactions else throw elseError
    } yield action
  }

  def triggerFailureWhen(project: Project, query: SQLActionBuilder, table: String) = {

    (sql"select case" ++
      sql"when exists( " ++ query ++ sql" )" ++
      sql"then 1" ++
      sql"else (select COLUMN_NAME" ++
      sql"from information_schema.columns" ++
      sql"where table_schema = ${project.id} AND TABLE_NAME = $table)end;").as[Int]
  }

  def triggerFailureWhenNot(project: Project, query: SQLActionBuilder, table: String) = {

    (sql"select case" ++
      sql"when not exists( " ++ query ++ sql" )" ++
      sql"then 1" ++
      sql"else (select COLUMN_NAME" ++
      sql"from information_schema.columns" ++
      sql"where table_schema = ${project.id} AND TABLE_NAME = $table)end;").as[Int]
  }

  // PATH QUERY

  def pathQuery(project: Project, path: Path) = {}

  //RESET DATA

  //only use transactionally in this order
  def disableForeignKeyConstraintChecks                   = sqlu"SET FOREIGN_KEY_CHECKS=0"
  def truncateTable(projectId: String, tableName: String) = sqlu"TRUNCATE TABLE `#$projectId`.`#$tableName`"
  def enableForeignKeyConstraintChecks                    = sqlu"SET FOREIGN_KEY_CHECKS=1"

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
