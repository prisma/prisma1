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
      sql"where not exists " ++ idFromWhere(projectId, where) ++ sql";").asUpdate
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
    (sql"insert into `#$projectId`.`#${parentInfo.relation.id}` (`id`, `#$parentSide`, `#$childSide`)" ++
      sql"Select '#$relationId'," ++ idFromWhere(projectId, parentInfo.where) ++ sql"," ++
      sql"`id` FROM `#$projectId`.`#${where.model.name}` where `#${where.field.name}` = ${where.fieldValue}" ++
      sql"on duplicate key update `#$projectId`.`#${parentInfo.relation.id}`.id=`#$projectId`.`#${parentInfo.relation.id}`.id").asUpdate
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
    (sql"DELETE FROM `#$projectId`.`_RelayId` WHERE `id`" ++ idFromWhereEquals(projectId, where)).asUpdate

  def deleteRelationRowByParent(projectId: String, parentInfo: ParentInfo) = {
    (sql"DELETE FROM `#$projectId`.`#${parentInfo.relation.id}` WHERE `#${parentInfo.field.relationSide.get}` = " ++ idFromWhere(projectId, parentInfo.where)).asUpdate
  }

  def deleteRelationRowByChild(projectId: String, relation: Relation, where: NodeSelector) = {
    (sql"DELETE FROM `#$projectId`.`#${relation.id}` WHERE `#${relation.sideOf(where.model)}`" ++ idFromWhereEquals(projectId, where)).asUpdate
  }

  def deleteRelationRowByParentAndChild(projectId: String, parentInfo: ParentInfo, where: NodeSelector) = {

    (sql"delete from `#$projectId`.`#${parentInfo.relation.id}` " ++
      sql"where `#${parentInfo.field.oppositeRelationSide.get}` = " ++ idFromWhere(projectId, where) ++
      sql" AND `#${parentInfo.field.relationSide.get}`" ++ idFromWhereEquals(projectId, parentInfo.where)).asUpdate
  }

  //CASCADING DELETE

  def cascadingDeleteChildActions(projectId: String, path: Path) = {
    val deleteRelayIds  = (sql"DELETE FROM `#$projectId`.`_RelayId` WHERE `id` IN " ++ pathQuery(projectId, path)).asUpdate
    val deleteDataItems = (sql"DELETE FROM `#$projectId`.`#${path.lastModel.name}` WHERE `id` IN " ++ pathQuery(projectId, path)).asUpdate
    DBIO.seq(deleteRelayIds, deleteDataItems)
  }

  def oldParentFailureTriggerByPath(project: Project, relation: Relation, path: Path) = {
    val query = sql"SELECT `id`" ++
      sql"FROM `#${project.id}`.`#${relation.id}` OLDPARENTPATHFAILURETRIGGER" ++
      sql"WHERE `#${relation.sideOf(path.lastModel)}` IN " ++ pathQuery(project.id, path)

    triggerFailureWhenExists(project, query, relation.id)
  }

  //SCALAR LISTS

  def setScalarList(projectId: String, where: NodeSelector, fieldName: String, values: Vector[Any]) = {
    val escapedValueTuples = for {
      (escapedValue, position) <- values.map(escapeUnsafeParam).zip((1 to values.length).map(_ * 1000))
    } yield {
      sql"(@nodeId, $position, " ++ escapedValue ++ sql")"
    }

    DBIO.seq(
      (sql"set @nodeId := " ++ idFromWhere(projectId, where)).asUpdate,
      sqlu"""delete from `#$projectId`.`#${where.model.name}_#${fieldName}` where nodeId = @nodeId""",
      (sql"insert into `#$projectId`.`#${where.model.name}_#${fieldName}` (`nodeId`, `position`, `value`) values " concat combineByComma(escapedValueTuples)).asUpdate
    )
  }

  def setScalarListToEmpty(projectId: String, where: NodeSelector, fieldName: String) = {
    (sql"DELETE FROM `#$projectId`.`#${where.model.name}_#${fieldName}` WHERE `nodeId`" ++ idFromWhereEquals(projectId, where)).asUpdate
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

  //HELPERS
  // Todo We could save a ton of queries here if we do not run extra queries if it is a IdNodeSelector Probably only works on =

  def idFromWhere(projectId: String, where: NodeSelector): SQLActionBuilder = {
    sql"(SELECT `id` FROM `#$projectId`.`#${where.model.name}` WHERE `#${where.field.name}` = ${where.fieldValue})"
  }

  def idFromWhereIn(projectId: String, where: NodeSelector): SQLActionBuilder = {
    sql"IN " ++ idFromWhere(projectId, where)
  }

  def idFromWhereEquals(projectId: String, where: NodeSelector): SQLActionBuilder = {
    where.isId match {
      case true  => sql" = ${where.fieldValue}"
      case false => sql" = " ++ idFromWhere(projectId, where)
    }
  }

  def idFromWherePath(projectId: String, where: NodeSelector): SQLActionBuilder = {
    sql"(SELECT `id` FROM (SELECT  * From `#$projectId`.`#${where.model.name}`) IDFROMWHEREPATH WHERE `#${where.field.name}` = ${where.fieldValue})"
  }

  def whereFailureTrigger(project: Project, where: NodeSelector) = {
    val table = where.model.name
    val query = sql"(SELECT `id` FROM `#${project.id}`.`#${where.model.name}` WHEREFAILURETRIGGER WHERE `#${where.field.name}` = ${where.fieldValue})"

    triggerFailureWhenNotExists(project, query, table)
  }

  def connectionFailureTrigger(project: Project, parentInfo: ParentInfo, where: NodeSelector) = {
    val childSide  = parentInfo.relation.sideOf(where.model)
    val parentSide = parentInfo.relation.sideOf(parentInfo.model)
    val table      = parentInfo.relation.id
    val query = sql"SELECT `id` FROM `#${project.id}`.`#$table` CONNECTIONFAILURETRIGGER" ++
      sql"WHERE `#$childSide` " ++ idFromWhereEquals(project.id, where) ++
      sql"AND `#$parentSide` " ++ idFromWhereEquals(project.id, parentInfo.where)

    triggerFailureWhenNotExists(project, query, table)
  }

  def oldParentFailureTriggerForRequiredRelations(project: Project, relation: Relation, where: NodeSelector) = {
    val childSide = relation.sideOf(where.model)
    val table     = relation.id
    val query     = sql"SELECT `id` FROM `#${project.id}`.`#$table` OLDPARENTFAILURETRIGGER WHERE `#$childSide`" ++ idFromWhereEquals(project.id, where)

    triggerFailureWhenExists(project, query, table)
  }

  def oldChildFailureTriggerForRequiredRelations(project: Project, parentInfo: ParentInfo) = {
    val parentSide = parentInfo.relation.sideOf(parentInfo.model)
    val table      = parentInfo.relation.id
    val query      = sql"SELECT `id` FROM `#${project.id}`.`#$table` OLDCHILDFAILURETRIGGER WHERE `#$parentSide`" ++ idFromWhereEquals(project.id, parentInfo.where)

    triggerFailureWhenExists(project, query, table)
  }

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

  def triggerFailureWhenNotExists(project: Project, query: SQLActionBuilder, table: String) = {

    (sql"select case" ++
      sql"when exists( " ++ query ++ sql" )" ++
      sql"then 1" ++
      sql"else (select COLUMN_NAME" ++
      sql"from information_schema.columns" ++
      sql"where table_schema = ${project.id} and TABLE_NAME = $table)end;").as[Int]
  }

  def triggerFailureWhenExists(project: Project, query: SQLActionBuilder, table: String) = {

    (sql"select case" ++
      sql"when not exists( " ++ query ++ sql" )" ++
      sql"then 1" ++
      sql"else (select COLUMN_NAME" ++
      sql"from information_schema.columns" ++
      sql"where table_schema = ${project.id} AND TABLE_NAME = $table)end;").as[Int]
  }

  def pathQuery(projectId: String, path: Path): SQLActionBuilder = {
    path.mwrs match {
      case x if x.isEmpty =>
        idFromWherePath(projectId, path.where)

      case x if x.nonEmpty =>
        val last = x.reverse.head
        sql"(SELECT `#${last.relation.sideOf(last.child)}`" ++
          sql" FROM (SELECT * FROM `#$projectId`.`#${last.relation.id}`) PATHQUERY" ++
          sql" WHERE `#${last.relation.sideOf(last.parent)}` IN " ++ pathQuery(projectId, path.cutOne) ++ sql")"
    }
  }

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
