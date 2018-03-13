package com.prisma.api.database

import com.prisma.api.database.SlickExtensions._
import com.prisma.api.database.Types.DataItemFilterCollection
import com.prisma.api.database.mutactions.mutactions.NestedCreateRelationMutaction
import com.prisma.api.mutations.{CoolArgs}
import com.prisma.api.schema.GeneralError
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.api.connector._
import com.prisma.shared.models._
import cool.graph.cuid.Cuid
import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.SQLActionBuilder
import slick.sql.{SqlAction, SqlStreamingAction}

import scala.concurrent.ExecutionContext.Implicits.global

object DatabaseMutationBuilder {
  val implicitlyCreatedColumns = List("id", "createdAt", "updatedAt")

  // region CREATE

  private def combineKeysAndValuesSeparately(args: CoolArgs) = {
    val escapedKeyValueTuples = args.raw.toList.map(x => (escapeKey(x._1), escapeUnsafeParam(x._2)))
    val escapedKeys           = combineByComma(escapedKeyValueTuples.map(_._1))
    val escapedValues         = combineByComma(escapedKeyValueTuples.map(_._2))
    (escapedKeys, escapedValues)
  }

  def createDataItem(projectId: String, modelName: String, args: CoolArgs): SqlStreamingAction[Vector[Int], Int, Effect]#ResultAction[Int, NoStream, Effect] = {
    val (escapedKeys: Option[SQLActionBuilder], escapedValues: Option[SQLActionBuilder]) = combineKeysAndValuesSeparately(args)
    (sql"INSERT INTO `#$projectId`.`#$modelName` (" ++ escapedKeys ++ sql") VALUES (" ++ escapedValues ++ sql")").asUpdate
  }

  def createRelayRow(projectId: String, where: NodeSelector): SqlStreamingAction[Vector[Int], Int, Effect]#ResultAction[Int, NoStream, Effect] = {
    (sql"INSERT INTO `#$projectId`.`_RelayId` (`id`, `stableModelIdentifier`) VALUES (${where.fieldValue}, ${where.model.stableIdentifier})").asUpdate
  }

  def createRelationRow(projectId: String,
                        relationTableName: String,
                        id: String,
                        a: String,
                        b: String): SqlStreamingAction[Vector[Int], Int, Effect]#ResultAction[Int, NoStream, Effect] = {

    (sql"insert into `#$projectId`.`#$relationTableName` (" ++ combineByComma(List(sql"`id`, `A`, `B`")) ++ sql") values (" ++ combineByComma(
      List(sql"$id, $a, $b")) ++ sql") on duplicate key update id=id").asUpdate
  }

  def createRelationRowByPath(projectId: String, path: Path): SqlAction[Int, NoStream, Effect] = {
    val childWhere = path.lastEdge_! match {
      case _: ModelEdge   => sys.error("Needs to be a node edge.")
      case edge: NodeEdge => edge.childWhere
    }
    val relationId = Cuid.createCuid()
    (sql"insert into `#$projectId`.`#${path.lastRelation_!.id}` (`id`, `#${path.parentSideOfLastEdge}`, `#${path.childSideOfLastEdge}`)" ++
      sql"Select '#$relationId'," ++ pathQueryForLastChild(projectId, path.removeLastEdge) ++ sql"," ++
      sql"`id` FROM `#$projectId`.`#${childWhere.model.name}` where `#${childWhere.field.name}` = ${childWhere.fieldValue}" ++
      sql"on duplicate key update `#$projectId`.`#${path.lastRelation_!.id}`.id = `#$projectId`.`#${path.lastRelation_!.id}`.id").asUpdate
  }

  //endregion

  //region UPDATE

  def updateDataItems(projectId: String, model: Model, args: CoolArgs, whereFilter: DataItemFilterCollection) = {
    val updateValues = combineByComma(args.raw.map { case (k, v) => escapeKey(k) ++ sql" = " ++ escapeUnsafeParam(v) })
    val whereSql     = QueryArguments.generateFilterConditions(projectId, model.name, whereFilter)
    (sql"UPDATE `#${projectId}`.`#${model.name}`" ++ sql"SET " ++ updateValues ++ prefixIfNotNone("where", whereSql)).asUpdate
  }

  def updateDataItemByUnique(projectId: String, where: NodeSelector, updateArgs: CoolArgs) = {
    val updateValues = combineByComma(updateArgs.raw.map { case (k, v) => escapeKey(k) ++ sql" = " ++ escapeUnsafeParam(v) })
    if (updateArgs.isNonEmpty) {
      (sql"UPDATE `#${projectId}`.`#${where.model.name}`" ++
        sql"SET " ++ updateValues ++
        sql"WHERE `#${where.field.name}` = ${where.fieldValue};").asUpdate
    } else {
      DBIOAction.successful(())
    }
  }

  def updateDataItemByPath(projectId: String, path: Path, updateArgs: CoolArgs) = {
    val updateValues = combineByComma(updateArgs.raw.map { case (k, v) => escapeKey(k) ++ sql" = " ++ escapeUnsafeParam(v) })
    val lastChildWhere = path.lastEdge_! match {
      case edge: NodeEdge => sql" `#${path.childSideOfLastEdge}`" ++ idFromWhereEquals(projectId, edge.childWhere) ++ sql" AND "
      case _: ModelEdge   => sql""
    }

    if (updateArgs.isNonEmpty) {
      val res = sql"UPDATE `#${projectId}`.`#${path.lastModel.name}`" ++
        sql"SET " ++ updateValues ++
        sql"WHERE `id` = (SELECT `#${path.childSideOfLastEdge}` " ++
        sql"FROM `#${projectId}`.`#${path.lastRelation_!.id}`" ++
        sql"WHERE" ++ lastChildWhere ++ sql"`#${path.parentSideOfLastEdge}` = " ++ pathQueryForLastParent(projectId, path) ++ sql")"
      res.asUpdate
    } else {
      DBIOAction.successful(())
    }
  }
  //endregion

  //region UPSERT

  def upsert(projectId: String,
             path: Path,
             createWhere: NodeSelector,
             createArgs: CoolArgs,
             updateArgs: CoolArgs,
             create: Vector[DBIOAction[Any, NoStream, Effect]],
             update: Vector[DBIOAction[Any, NoStream, Effect]]) = {

    val q       = DatabaseQueryBuilder.existsByPath(projectId, path).as[Boolean]
    val qInsert = DBIOAction.seq(createDataItem(projectId, path.lastModel.name, createArgs), createRelayRow(projectId, createWhere), DBIOAction.seq(create: _*))
    val qUpdate = DBIOAction.seq(updateDataItemByUnique(projectId, path.root, updateArgs), DBIOAction.seq(update: _*))

    ifThenElse(q, qUpdate, qInsert)
  }

  def upsertIfInRelationWith(
      project: Project,
      path: Path,
      createWhere: NodeSelector,
      createArgs: CoolArgs,
      updateArgs: CoolArgs,
      create: Vector[DBIOAction[Any, NoStream, Effect]],
      update: Vector[DBIOAction[Any, NoStream, Effect]],
      relationMutactions: NestedCreateRelationMutaction,
  ) = {

    val q = DatabaseQueryBuilder.existsNodeIsInRelationshipWith(project, path).as[Boolean]
    val qInsert =
      DBIOAction.seq(
        createDataItem(project.id, path.lastModel.name, createArgs),
        createRelayRow(project.id, createWhere),
        DBIOAction.seq(relationMutactions.allActions: _*),
        DBIOAction.seq(create: _*)
      )
    val qUpdate = DBIOAction.seq(updateDataItemByPath(project.id, path, updateArgs), DBIOAction.seq(update: _*))

    ifThenElseNestedUpsert(q, qUpdate, qInsert)
  }
  //endregion

  //region DELETE

  def deleteDataItems(project: Project, model: Model, whereFilter: DataItemFilterCollection) = {
    val whereSql = QueryArguments.generateFilterConditions(project.id, model.name, whereFilter)
    (sql"DELETE FROM `#${project.id}`.`#${model.name}`" ++ prefixIfNotNone("where", whereSql)).asUpdate
  }

  def deleteRelayIds(project: Project, model: Model, whereFilter: DataItemFilterCollection) = {
    val whereSql = QueryArguments.generateFilterConditions(project.id, model.name, whereFilter)
    (sql"DELETE FROM `#${project.id}`.`_RelayId`" ++
      (sql"WHERE `id` IN (" ++
        sql"SELECT `id`" ++
        sql"FROM `#${project.id}`.`#${model.name}`" ++
        prefixIfNotNone("where", whereSql) ++ sql")")).asUpdate
  }

  def deleteDataItem(projectId: String, path: Path) =
    (sql"DELETE FROM `#$projectId`.`#${path.lastModel.name}` WHERE `id` = " ++ pathQueryForLastChild(projectId, path)).asUpdate

  def deleteRelayRow(projectId: String, path: Path) =
    (sql"DELETE FROM `#$projectId`.`_RelayId` WHERE `id` =" ++ pathQueryForLastChild(projectId, path)).asUpdate

  def deleteRelationRowByParent(projectId: String, path: Path) = {
    (sql"DELETE FROM `#$projectId`.`#${path.lastRelation_!.id}` WHERE `#${path.parentSideOfLastEdge}` = " ++ pathQueryForLastParent(projectId, path)).asUpdate
  }

  def deleteRelationRowByChildWithWhere(projectId: String, path: Path) = {
    val where = path.lastEdge_! match {
      case _: ModelEdge   => sys.error("Should be a node Edge")
      case edge: NodeEdge => edge.childWhere

    }
    (sql"DELETE FROM `#$projectId`.`#${path.lastRelation_!.id}` WHERE `#${path.childSideOfLastEdge}`" ++ idFromWhereEquals(projectId, where)).asUpdate
  }

  def deleteRelationRowByParentAndChild(projectId: String, path: Path) = {
    (sql"DELETE FROM `#$projectId`.`#${path.lastRelation_!.id}` " ++
      sql"WHERE `#${path.childSideOfLastEdge}` = " ++ pathQueryForLastChild(projectId, path) ++
      sql" AND `#${path.parentSideOfLastEdge}` = " ++ pathQueryForLastParent(projectId, path)).asUpdate
  }

  def cascadingDeleteChildActions(projectId: String, path: Path) = {
    val deleteRelayIds  = (sql"DELETE FROM `#$projectId`.`_RelayId` WHERE `id` IN " ++ pathQueryForLastChild(projectId, path)).asUpdate
    val deleteDataItems = (sql"DELETE FROM `#$projectId`.`#${path.lastModel.name}` WHERE `id` IN " ++ pathQueryForLastChild(projectId, path)).asUpdate
    DBIO.seq(deleteRelayIds, deleteDataItems)
  }

  //endregion

  //region SCALAR LISTS

  def setScalarList(projectId: String, path: Path, fieldName: String, values: Vector[Any]) = {
    val escapedValueTuples = for {
      (escapedValue, position) <- values.map(escapeUnsafeParam).zip((1 to values.length).map(_ * 1000))
    } yield {
      sql"(@nodeId, $position, " ++ escapedValue ++ sql")"
    }

    DBIO.seq(
      (sql"set @nodeId := " ++ pathQueryForLastChild(projectId, path)).asUpdate,
      sqlu"""delete from `#$projectId`.`#${path.lastModel.name}_#${fieldName}` where nodeId = @nodeId""",
      (sql"insert into `#$projectId`.`#${path.lastModel.name}_#${fieldName}` (`nodeId`, `position`, `value`) values " concat combineByComma(escapedValueTuples)).asUpdate
    )
  }

  def setScalarListToEmpty(projectId: String, path: Path, fieldName: String) = {
    (sql"DELETE FROM `#$projectId`.`#${path.lastModel.name}_#${fieldName}` WHERE `nodeId` = " ++ pathQueryForLastChild(projectId, path)).asUpdate
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

  def getDbActionsForUpsertScalarLists(projectId: String, path: Path, args: CoolArgs): Vector[DBIOAction[Any, NoStream, Effect]] = {
    val x = for {
      field  <- path.lastModel.scalarListFields
      values <- args.subScalarList(field)
    } yield {
      values.values.isEmpty match {
        case true  => setScalarListToEmpty(projectId, path, field.name)
        case false => setScalarList(projectId, path, field.name, values.values)
      }
    }
    x.toVector
  }
  //endregion

  //region RESET DATA

  // todo roll this into one query
  def disableForeignKeyConstraintChecks                   = sqlu"SET FOREIGN_KEY_CHECKS=0"
  def truncateTable(projectId: String, tableName: String) = sqlu"TRUNCATE TABLE `#$projectId`.`#$tableName`"
  def enableForeignKeyConstraintChecks                    = sqlu"SET FOREIGN_KEY_CHECKS=1"

  //endregion

  // region HELPERS

  def idFromWhere(projectId: String, where: NodeSelector): SQLActionBuilder = {
    sql"(SELECT `id` FROM (SELECT * FROM `#$projectId`.`#${where.model.name}`) IDFROMWHERE WHERE `#${where.field.name}` = ${where.fieldValue})"
  }

  def idFromWhereEquals(projectId: String, where: NodeSelector): SQLActionBuilder = where.isId match {
    case true  => sql" = ${where.fieldValue}"
    case false => sql" = " ++ idFromWhere(projectId, where)
  }

  def idFromWherePath(projectId: String, where: NodeSelector): SQLActionBuilder = {
    sql"(SELECT `id` FROM (SELECT  * From `#$projectId`.`#${where.model.name}`) IDFROMWHEREPATH WHERE `#${where.field.name}` = ${where.fieldValue})"
  }

  //we could probably save even more joins if we start the paths always at the last node edge

  def pathQueryForLastParent(projectId: String, path: Path): SQLActionBuilder = pathQueryForLastChild(projectId, path.removeLastEdge)

  def pathQueryForLastChild(projectId: String, path: Path): SQLActionBuilder = {
    path.edges match {
      case Nil                                => idFromWhere(projectId, path.root)
      case x if x.last.isInstanceOf[NodeEdge] => idFromWhere(projectId, x.last.asInstanceOf[NodeEdge].childWhere)
      case _                                  => pathQueryThatUsesWholePath(projectId, path)
    }
  }

  object ::> { def unapply[A](l: List[A]) = Some((l.init, l.last)) }

  def pathQueryThatUsesWholePath(projectId: String, path: Path): SQLActionBuilder = {
    path.edges match {
      case Nil =>
        idFromWherePath(projectId, path.root)

      case _ ::> last =>
        val childWhere = last match {
          case edge: NodeEdge => sql" `#${edge.childRelationSide}`" ++ idFromWhereEquals(projectId, edge.childWhere) ++ sql" AND "
          case _: ModelEdge   => sql""
        }

        sql"(SELECT `#${last.childRelationSide}`" ++
          sql" FROM (SELECT * FROM `#$projectId`.`#${last.relation.id}`) PATHQUERY" ++
          sql" WHERE " ++ childWhere ++ sql"`#${last.parentRelationSide}` IN " ++ pathQueryForLastParent(projectId, path) ++ sql")"
    }
  }

  def whereFailureTrigger(project: Project, where: NodeSelector) = {
    val table = where.model.name
    val query = sql"(SELECT `id` FROM `#${project.id}`.`#${where.model.name}` WHEREFAILURETRIGGER WHERE `#${where.field.name}` = ${where.fieldValue})"

    triggerFailureWhenNotExists(project, query, table)
  }

  def connectionFailureTrigger(project: Project, path: Path) = {
    val table = path.lastRelation.get.id

    val lastChildWhere = path.lastEdge_! match {
      case edge: NodeEdge => sql" `#${path.childSideOfLastEdge}`" ++ idFromWhereEquals(project.id, edge.childWhere) ++ sql" AND "
      case _: ModelEdge   => sql""
    }

    val query =
      sql"SELECT `id` FROM `#${project.id}`.`#$table` CONNECTIONFAILURETRIGGERPATH" ++
        sql"WHERE" ++ lastChildWhere ++ sql"`#${path.parentSideOfLastEdge}` = " ++ pathQueryForLastParent(project.id, path)

    triggerFailureWhenNotExists(project, query, table)
  }

  def oldParentFailureTriggerForRequiredRelations(project: Project, relation: Relation, where: NodeSelector, childSide: RelationSide.Value) = {
    val table = relation.id
    val query = sql"SELECT `id` FROM `#${project.id}`.`#$table` OLDPARENTFAILURETRIGGER WHERE `#$childSide` " ++ idFromWhereEquals(project.id, where)

    triggerFailureWhenExists(project, query, table)
  }

  def oldParentFailureTrigger(project: Project, path: Path) = {
    val table = path.lastRelation_!.id
    val query = sql"SELECT `id` FROM `#${project.id}`.`#$table` OLDPARENTPATHFAILURETRIGGER WHERE `#${path.childSideOfLastEdge}` IN " ++ pathQueryForLastChild(
      project.id,
      path)
    triggerFailureWhenExists(project, query, table)
  }

  def oldParentFailureTriggerByField(project: Project, path: Path, field: Field) = {
    val table = field.relation.get.id
    val query = sql"SELECT `id` FROM `#${project.id}`.`#$table` OLDPARENTPATHFAILURETRIGGERBYFIELD WHERE `#${field.oppositeRelationSide.get}` IN " ++ pathQueryForLastChild(
      project.id,
      path)
    triggerFailureWhenExists(project, query, table)
  }

  def oldParentFailureTriggerByFieldAndFilter(project: Project, model: Model, filter: DataItemFilterCollection, field: Field) = {
    val table = field.relation.get.id
    val whereSql = QueryArguments.generateFilterConditions(project.id, model.name, filter) match {
      case None    => sql""
      case Some(x) => sql"WHERE " ++ x
    }

    val query = sql"SELECT `id` FROM `#${project.id}`.`#$table` OLDPARENTPATHFAILURETRIGGERBYFIELDANDFILTER WHERE `#${field.oppositeRelationSide.get}` IN (SELECT `id` FROM `#${project.id}`.`#${model.name}` " ++ whereSql ++ sql")"
    triggerFailureWhenExists(project, query, table)
  }

  def oldChildFailureTrigger(project: Project, path: Path) = {
    val table = path.lastRelation_!.id
    val query = sql"SELECT `id` FROM `#${project.id}`.`#$table` OLDCHILDPATHFAILURETRIGGER WHERE `#${path.parentSideOfLastEdge}` IN " ++ pathQueryForLastParent(
      project.id,
      path)
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

  def ifThenElseNestedUpsert(condition: SqlStreamingAction[Vector[Boolean], Boolean, Effect],
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
  def triggerFailureWhenExists(project: Project, query: SQLActionBuilder, table: String)    = triggerFailureInternal(project, query, table, notExists = false)
  def triggerFailureWhenNotExists(project: Project, query: SQLActionBuilder, table: String) = triggerFailureInternal(project, query, table, notExists = true)

  private def triggerFailureInternal(project: Project, query: SQLActionBuilder, table: String, notExists: Boolean) = {
    val notValue = if (notExists) sql"" else sql"not"

    (sql"select case" ++
      sql"when" ++ notValue ++ sql"exists( " ++ query ++ sql" )" ++
      sql"then 1" ++
      sql"else (select COLUMN_NAME" ++
      sql"from information_schema.columns" ++
      sql"where table_schema = ${project.id} AND TABLE_NAME = $table)end;").as[Int]
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
  //endregion
}
