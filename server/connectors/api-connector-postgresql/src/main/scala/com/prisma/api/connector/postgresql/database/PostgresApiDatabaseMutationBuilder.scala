package com.prisma.api.connector.postgresql.database

import java.sql.{PreparedStatement, Statement}
import java.util.Date

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.JdbcExtensions._
import com.prisma.api.connector.postgresql.database.SlickExtensions._
import com.prisma.api.schema.UserFacingError
import com.prisma.gc_values.{GCValue, GCValueExtractor, ListGCValue, NullGCValue}
import com.prisma.shared.models.Manifestations.RelationTableManifestation
import com.prisma.gc_values._
import com.prisma.shared.models._
import cool.graph.cuid.Cuid
import org.joda.time.{DateTime, DateTimeZone}
import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.SQLActionBuilder
import slick.sql.{SqlAction, SqlStreamingAction}

case class PostgresApiDatabaseMutationBuilder(
    schemaName: String,
    schema: Schema,
) {

  // region CREATE

  def createDataItem(path: Path, args: PrismaArgs): DBIO[CreateDataItemResult] = {

    SimpleDBIO[CreateDataItemResult] { x =>
      val argsAsRoot   = args.raw.asRoot
      val fields       = path.lastModel.fields.filter(field => argsAsRoot.hasArgFor(field.name))
      val columns      = fields.map(_.dbName)
      val escapedKeys  = columns.map(column => s""""$column"""").mkString(",")
      val placeHolders = columns.map(_ => "?").mkString(",")

      val query                         = s"""INSERT INTO "$schemaName"."${path.lastModel.dbName}" ($escapedKeys) VALUES ($placeHolders)"""
      val itemInsert: PreparedStatement = x.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)

      fields.map(_.name).zipWithIndex.foreach {
        case (column, index) =>
          argsAsRoot.map.get(column) match {
            case Some(NullGCValue) if column == "createdAt" || column == "updatedAt" => itemInsert.setTimestamp(index + 1, currentTimeStampUTC)
            case Some(gCValue)                                                       => itemInsert.setGcValue(index + 1, gCValue)
            case None if column == "createdAt" || column == "updatedAt"              => itemInsert.setTimestamp(index + 1, currentTimeStampUTC)
            case None                                                                => itemInsert.setNull(index + 1, java.sql.Types.NULL)
          }
      }
      itemInsert.execute()

      val generatedKeys = itemInsert.getGeneratedKeys()
      generatedKeys.next()
      // fixme: the name of the id field might be different?
      val field = path.lastModel.getFieldByName_!("id")
      CreateDataItemResult(generatedKeys.getGcValue(field.dbName, field.typeIdentifier))
    }
  }

  def createRelayRow(path: Path): SqlStreamingAction[Vector[Int], Int, Effect]#ResultAction[Int, NoStream, Effect] = {
    val where = path.lastCreateWhere_!
    (sql"""INSERT INTO "#$schemaName"."_RelayId" ("id", "stableModelIdentifier") VALUES (${where.fieldValue}, ${where.model.stableIdentifier})""").asUpdate
  }

  def createRelationRowByPath(schema: Schema, path: Path): SqlAction[Int, NoStream, Effect] = {
    val relation = path.lastRelation_!
    val childWhere = path.lastEdge_! match {
      case _: ModelEdge   => sys.error("Needs to be a node edge.")
      case edge: NodeEdge => edge.childWhere
    }

    if (relation.isInlineRelation) {
      val inlineManifestation = relation.inlineManifestation.get
      val referencingColumn   = inlineManifestation.referencingColumn
      val tableName           = relation.relationTableNameNew(schema)

      (sql"""update "#$schemaName"."#${tableName}" """ ++
        sql"""set "#${referencingColumn}" = subquery.id""" ++
        sql"""from (select "id" from "#$schemaName"."#${childWhere.model.dbName}" where "#${childWhere.field.name}" = ${childWhere.fieldValue}) as subquery""" ++
        sql"""where "#$schemaName"."#${tableName}".id = """ ++ pathQueryForLastChild(path.removeLastEdge)).asUpdate

    } else if (relation.hasManifestation) {
      val nodeEdge        = path.lastEdge_!.asInstanceOf[NodeEdge]
      val parentModel     = nodeEdge.parent
      val childModel      = nodeEdge.child
      val manifestation   = relation.manifestation.get.asInstanceOf[RelationTableManifestation]
      val columnForParent = if (parentModel.id == relation.modelAId) manifestation.modelAColumn else manifestation.modelBColumn
      val columnForChild  = if (childModel.id == relation.modelAId) manifestation.modelAColumn else manifestation.modelBColumn

      (sql"""insert into "#$schemaName"."#${path.lastRelation_!.relationTableName}" ("#$columnForParent", "#$columnForChild")""" ++
        sql"""Select """ ++ pathQueryForLastChild(path.removeLastEdge) ++ sql"," ++
        sql""""id" FROM "#$schemaName"."#${childWhere.model.dbName}" where "#${childWhere.field.name}" = ${childWhere.fieldValue}""").asUpdate
    } else {
      val relationId = Cuid.createCuid()
      (sql"""insert into "#$schemaName"."#${path.lastRelation_!.relationTableName}" ("id", "#${path.parentSideOfLastEdge}", "#${path.childSideOfLastEdge}")""" ++
        sql"""Select '#$relationId',""" ++ pathQueryForLastChild(path.removeLastEdge) ++ sql""","id" """ ++
        sql"""FROM "#$schemaName"."#${childWhere.model.name}" where "#${childWhere.field.name}" = ${childWhere.fieldValue}""").asUpdate
    }
//    https://stackoverflow.com/questions/1109061/insert-on-duplicate-update-in-postgresql
//    ++
//      sql"on conflict (id )  key update #$databaseName.#${path.lastRelation_!.relationTableName}.id = #$databaseName.#${path.lastRelation_!.relationTableName}.id").asUpdate
  }

  //endregion

  //region UPDATE

  def updateDataItems(model: Model, args: PrismaArgs, whereFilter: Option[DataItemFilterCollection]) = {
    val updateValues = combineByComma(args.raw.asRoot.map.map { case (k, v) => escapeKey(k) ++ sql" = $v" })

    if (updateValues.isDefined) {
      (sql"""UPDATE "#$schemaName"."#${model.name}"""" ++ sql"SET " ++ addUpdatedDateTime(updateValues) ++ whereFilterAppendix(schemaName,
                                                                                                                               model.name,
                                                                                                                               whereFilter)).asUpdate
    } else {
      DBIOAction.successful(())
    }
  }

  def updateDataItemByPath(path: Path, updateArgs: PrismaArgs) = {
    val updateValues = combineByComma(updateArgs.raw.asRoot.map.map { case (k, v) => escapeKey(k) ++ sql" = $v" })
    def fromEdge(edge: Edge) = edge match {
      case edge: NodeEdge => sql""" "#${path.childSideOfLastEdge}"""" ++ idFromWhereEquals(edge.childWhere) ++ sql" AND "
      case _: ModelEdge   => sql""
    }

    val baseQuery = sql"""UPDATE "#$schemaName"."#${path.lastModel.name}" SET """ ++ addUpdatedDateTime(updateValues) ++ sql"""WHERE "id" ="""

    if (updateArgs.raw.asRoot.map.isEmpty) {
      DBIOAction.successful(())
    } else {

      val query = path.lastEdge match {
        case Some(edge) =>
          baseQuery ++ sql"""(SELECT "#${path.childSideOfLastEdge}" """ ++
            sql"""FROM "#$schemaName"."#${path.lastRelation_!.relationTableName}"""" ++
            sql"WHERE" ++ fromEdge(edge) ++ sql""""#${path.parentSideOfLastEdge}" = """ ++ pathQueryForLastParent(path) ++ sql")"
        case None => baseQuery ++ idFromWhere(path.root)
      }
      query.asUpdate
    }
  }

  //endregion

  //region UPSERT
  private def addUpdatedDateTime(updateValues: Option[SQLActionBuilder]) = {
    val today              = new Date()
    val exactlyNow         = new DateTime(today).withZone(DateTimeZone.UTC)
    val currentDateGCValue = DateTimeGCValue(exactlyNow)
    val updatedAt          = sql""""updatedAt" = $currentDateGCValue """
    combineByComma(updateValues ++ List(updatedAt))
  }

  def upsert(
      createPath: Path,
      updatePath: Path,
      createArgs: PrismaArgs,
      updateArgs: PrismaArgs,
      create: slick.dbio.DBIOAction[Unit, slick.dbio.NoStream, slick.dbio.Effect with slick.dbio.Effect.All],
      update: slick.dbio.DBIOAction[Unit, slick.dbio.NoStream, slick.dbio.Effect with slick.dbio.Effect.All]
  ) = {

    val query     = sql"""select exists ( SELECT "id" FROM "#$schemaName"."#${updatePath.lastModel.name}" WHERE "id" = """ ++ pathQueryForLastChild(updatePath) ++ sql")"
    val condition = query.as[Boolean]
    // insert creates item first, then the list values
    val qInsert = DBIOAction.seq(createDataItem(createPath, createArgs), createRelayRow(createPath), create)
    // update first sets the lists, then updates the item
    val qUpdate = DBIOAction.seq(update, updateDataItemByPath(updatePath, updateArgs))

    ifThenElse(condition, qUpdate, qInsert)
  }

  def upsertIfInRelationWith(
      createPath: Path,
      updatePath: Path,
      createArgs: PrismaArgs,
      updateArgs: PrismaArgs,
      scalarListCreate: DBIO[_],
      scalarListUpdate: DBIO[_],
      createCheck: DBIO[_],
  ) = {

    def existsNodeIsInRelationshipWith = {
      def nodeSelector(last: Edge) = last match {
        case edge: NodeEdge => sql" id" ++ idFromWhereEquals(edge.childWhere) ++ sql" AND "
        case _: ModelEdge   => sql""
      }

      sql"""select EXISTS (
            select "id" from "#$schemaName"."#${updatePath.lastModel.name}"
            where""" ++ nodeSelector(updatePath.lastEdge_!) ++
        sql""" "id" IN""" ++ pathQueryThatUsesWholePath(updatePath) ++ sql")"
    }

    val condition = existsNodeIsInRelationshipWith.as[Boolean]
    //insert creates item first and then the listvalues
    val qInsert = DBIOAction.seq(createDataItem(createPath, createArgs), createRelayRow(createPath), createCheck, scalarListCreate)
    //update updates list values first and then the item
    val qUpdate = DBIOAction.seq(scalarListUpdate, updateDataItemByPath(updatePath, updateArgs))

    ifThenElseNestedUpsert(condition, qUpdate, qInsert)
  }

  //endregion

  //region DELETE

  def deleteDataItems(model: Model, whereFilter: Option[DataItemFilterCollection]) = {
    (sql"""DELETE FROM "#$schemaName"."#${model.name}"""" ++ whereFilterAppendix(schemaName, model.name, whereFilter)).asUpdate
  }

  def deleteRelayIds(model: Model, whereFilter: Option[DataItemFilterCollection]) = {
    (sql"""DELETE FROM "#$schemaName"."_RelayId" WHERE "id" IN ( SELECT "id" FROM "#$schemaName"."#${model.name}"""" ++ whereFilterAppendix(
      schemaName,
      model.name,
      whereFilter) ++ sql")").asUpdate
  }

  def deleteDataItem(path: Path) =
    (sql"""DELETE FROM "#$schemaName"."#${path.lastModel.name}" WHERE "id" = """ ++ pathQueryForLastChild(path)).asUpdate

  def deleteRelayRow(path: Path) =
    (sql"""DELETE FROM "#$schemaName"."_RelayId" WHERE "id" = """ ++ pathQueryForLastChild(path)).asUpdate

  def deleteRelationRowByParent(schema: Schema, path: Path): DBIO[Unit] = {
    val relation = path.lastRelation_!
    if (relation.isInlineRelation) {
      dbioUnit
    } else {
      val relationTable = path.lastRelation_!.relationTableNameNew(schema)
      val action =
        (sql"""DELETE FROM "#$schemaName"."#${relationTable}" WHERE "#${path.columnForParentSideOfLastEdge}" = """ ++ pathQueryForLastParent(path)).asUpdate

      action.andThen(dbioUnit)
    }
  }

  def deleteRelationRowByChildWithWhere(schema: Schema, path: Path): DBIO[Unit] = {
    val relation = path.lastRelation_!
    if (relation.isInlineRelation) {
      dbioUnit
    } else {
      val relationTable = path.lastRelation_!.relationTableNameNew(schema)
      val where = path.lastEdge_! match {
        case _: ModelEdge   => sys.error("Should be a node Edge")
        case edge: NodeEdge => edge.childWhere

      }
      (sql"""DELETE FROM "#$schemaName"."#${relationTable}" WHERE "#${path.columnForChildSideOfLastEdge}"""" ++ idFromWhereEquals(where)).asUpdate
        .andThen(dbioUnit)
    }
  }

  def deleteRelationRowByParentAndChild(path: Path) = {
    (sql"""DELETE FROM "#$schemaName"."#${path.lastRelation_!.relationTableName}" """ ++
      sql"""WHERE "#${path.childSideOfLastEdge}" = """ ++ pathQueryForLastChild(path) ++
      sql""" AND "#${path.parentSideOfLastEdge}" = """ ++ pathQueryForLastParent(path)).asUpdate.andThen(dbioUnit)
  }

  def cascadingDeleteChildActions(path: Path) = {
    val deleteRelayIds = (sql"""DELETE FROM "#$schemaName"."_RelayId" WHERE "id" IN (""" ++ pathQueryForLastChild(path) ++ sql")").asUpdate
    val deleteDataItems =
      (sql"""DELETE FROM "#$schemaName"."#${path.lastModel.name}" WHERE "id" IN (""" ++ pathQueryForLastChild(path) ++ sql")").asUpdate
    DBIO.seq(deleteRelayIds, deleteDataItems)
  }

  //endregion

  //region SCALAR LISTS
  def setScalarList(path: Path, listFieldMap: Vector[(String, ListGCValue)]) = {
    val idQuery = (sql"""SELECT "id" FROM "#$schemaName"."#${path.lastModel.name}" WHERE "id" = """ ++ pathQueryForLastChild(path)).as[String]
    if (listFieldMap.isEmpty) DBIOAction.successful(()) else setManyScalarListHelper(path.lastModel, listFieldMap, idQuery)
  }

  def setManyScalarLists(model: Model, listFieldMap: Vector[(String, ListGCValue)], whereFilter: Option[DataItemFilterCollection]) = {
    val idQuery = (sql"""SELECT "id" FROM "#$schemaName"."#${model.name}"""" ++ whereFilterAppendix(schemaName, model.name, whereFilter)).as[String]
    if (listFieldMap.isEmpty) DBIOAction.successful(()) else setManyScalarListHelper(model, listFieldMap, idQuery)
  }

  def setManyScalarListHelper(model: Model, listFieldMap: Vector[(String, ListGCValue)], idQuery: SqlStreamingAction[Vector[String], String, Effect]) = {
    import scala.concurrent.ExecutionContext.Implicits.global

    def listInsert(ids: Vector[String]) = {
      if (ids.isEmpty) {
        DBIOAction.successful(())
      } else {

        SimpleDBIO[Unit] { x =>
          def valueTuplesForListField(listGCValue: ListGCValue) =
            for {
              nodeId                   <- ids
              (escapedValue, position) <- listGCValue.values.zip((1 to listGCValue.size).map(_ * 1000))
            } yield {
              (nodeId, position, escapedValue)
            }

          val whereString = ids.length match {
            case 1 => s""" WHERE "nodeId" =  '${ids.head}'"""
            case _ => s""" WHERE "nodeId" in ${ids.map(id => s"'$id'").mkString("(", ",", ")")}"""
          }

          listFieldMap.foreach {
            case (fieldName, listGCValue) =>
              val wipe                             = s"""DELETE  FROM "$schemaName"."${model.name}_$fieldName" $whereString"""
              val wipeOldValues: PreparedStatement = x.connection.prepareStatement(wipe)
              wipeOldValues.executeUpdate()

              val insert                             = s"""INSERT INTO "$schemaName"."${model.name}_$fieldName" ("nodeId", "position", "value") VALUES (?,?,?)"""
              val insertNewValues: PreparedStatement = x.connection.prepareStatement(insert)
              val newValueTuples                     = valueTuplesForListField(listGCValue)
              newValueTuples.foreach { tuple =>
                insertNewValues.setString(1, tuple._1)
                insertNewValues.setInt(2, tuple._2)
                insertNewValues.setGcValue(3, tuple._3)
                insertNewValues.addBatch()
              }
              insertNewValues.executeBatch()
          }
        }
      }
    }

    for {
      nodeIds <- idQuery
      action  <- listInsert(nodeIds)
    } yield action
  }

  //endregion

  //region RESET DATA
  def truncateTable(tableName: String) = sqlu"""TRUNCATE TABLE "#$schemaName"."#$tableName" CASCADE"""

  //endregion

  // region HELPERS

  def idFromWhere(where: NodeSelector): SQLActionBuilder = {
    if (where.isId) {
      sql"""${where.fieldValue}"""
    } else {
      sql"""(SELECT "id" FROM (SELECT * FROM "#$schemaName"."#${where.model.name}") IDFROMWHERE WHERE "#${where.field.name}" = ${where.fieldValue})"""
    }
  }

  def idFromWhereEquals(where: NodeSelector): SQLActionBuilder = sql" = " ++ idFromWhere(where)

  def idFromWherePath(where: NodeSelector): SQLActionBuilder = {
    sql"""(SELECT "id" FROM (SELECT  * From "#$schemaName"."#${where.model.name}") IDFROMWHEREPATH WHERE "#${where.field.name}" = ${where.fieldValue})"""
  }

  def pathQueryForLastParent(path: Path): SQLActionBuilder = pathQueryForLastChild(path.removeLastEdge)

  def pathQueryForLastChild(path: Path): SQLActionBuilder = {
    path.edges match {
      case Nil                                => idFromWhere(path.root)
      case x if x.last.isInstanceOf[NodeEdge] => idFromWhere(x.last.asInstanceOf[NodeEdge].childWhere)
      case _                                  => pathQueryThatUsesWholePath(path)
    }
  }

  object ::> { def unapply[A](l: List[A]) = Some((l.init, l.last)) }

  def pathQueryThatUsesWholePath(path: Path): SQLActionBuilder = {
    path.edges match {
      case Nil =>
        idFromWherePath(path.root)

      case _ ::> last =>
        val childWhere = last match {
          case edge: NodeEdge => sql""" "#${edge.columnForChildRelationSide}"""" ++ idFromWhereEquals(edge.childWhere) ++ sql" AND "
          case _: ModelEdge   => sql""
        }

        sql"""(SELECT "#${last.columnForChildRelationSide}"""" ++
          sql""" FROM (SELECT * FROM "#$schemaName"."#${last.relation.relationTableNameNew(schema)}") PATHQUERY""" ++
          sql" WHERE " ++ childWhere ++ sql""""#${last.columnForParentRelationSide}" IN (""" ++ pathQueryForLastParent(path) ++ sql"))"
    }
  }

  def whereFailureTrigger(where: NodeSelector, causeString: String) = {
    val table = where.model.name
    val query = sql"""(SELECT "id" FROM "#$schemaName"."#${where.model.name}" WHEREFAILURETRIGGER WHERE "#${where.field.name}" = ${where.fieldValue})"""

    triggerFailureWhenNotExists(query, table, causeString)
  }

  def connectionFailureTrigger(path: Path, causeString: String) = {
    val table = path.lastRelation.get.relationTableNameNew(schema)

    val lastChildWhere = path.lastEdge_! match {
      case edge: NodeEdge => sql""" "#${path.columnForChildSideOfLastEdge}"""" ++ idFromWhereEquals(edge.childWhere) ++ sql" AND "
      case _: ModelEdge   => sql""
    }

    val query =
      sql"""SELECT "id" FROM "#$schemaName"."#$table" CONNECTIONFAILURETRIGGERPATH""" ++
        sql"WHERE" ++ lastChildWhere ++ sql""""#${path.columnForParentSideOfLastEdge}" = """ ++ pathQueryForLastParent(path)

    triggerFailureWhenNotExists(query, table, causeString)
  }

  def oldParentFailureTriggerForRequiredRelations(relation: Relation,
                                                  where: NodeSelector,
                                                  childSide: RelationSide.Value,
                                                  triggerString: String): slick.sql.SqlStreamingAction[Vector[String], String, slick.dbio.Effect] = {
    val table = relation.relationTableName
    val query = sql"""SELECT "id" FROM "#$schemaName"."#$table" OLDPARENTFAILURETRIGGER WHERE "#$childSide" """ ++ idFromWhereEquals(where)

    triggerFailureWhenExists(query, table, triggerString)
  }

  def oldParentFailureTrigger(path: Path, triggerString: String) = {
    val table = path.lastRelation_!.relationTableName
    val query = sql"""SELECT "id" FROM "#$schemaName"."#$table" OLDPARENTPATHFAILURETRIGGER WHERE "#${path.childSideOfLastEdge}" IN (""" ++ pathQueryForLastChild(
      path) ++ sql")"
    triggerFailureWhenExists(query, table, triggerString)
  }

  def oldParentFailureTriggerByField(path: Path, field: Field, triggerString: String) = {
    val table = field.relation.get.relationTableName
    val query = sql"""SELECT "id" FROM "#$schemaName"."#$table" OLDPARENTPATHFAILURETRIGGERBYFIELD WHERE "#${field.oppositeRelationSide.get}" IN (""" ++ pathQueryForLastChild(
      path) ++ sql")"
    triggerFailureWhenExists(query, table, triggerString)
  }

  def oldParentFailureTriggerByFieldAndFilter(model: Model, whereFilter: Option[DataItemFilterCollection], field: Field, causeString: String) = {
    val table = field.relation.get.relationTableName
    val query = sql"""SELECT "id" FROM "#$schemaName"."#$table" OLDPARENTPATHFAILURETRIGGERBYFIELDANDFILTER""" ++
      sql"""WHERE "#${field.oppositeRelationSide.get}" IN (SELECT "id" FROM "#$schemaName"."#${model.name}" """ ++
      whereFilterAppendix(schemaName, model.name, whereFilter) ++ sql")"
    triggerFailureWhenExists(query, table, causeString)
  }

  def oldChildFailureTrigger(path: Path, triggerString: String) = {
    val table = path.lastRelation_!.relationTableName
    val query = sql"""SELECT "id" FROM "#$schemaName"."#$table" OLDCHILDPATHFAILURETRIGGER WHERE "#${path.parentSideOfLastEdge}" IN (""" ++ pathQueryForLastParent(
      path) ++ sql")"
    triggerFailureWhenExists(query, table, triggerString)
  }

  def ifThenElse(condition: SqlStreamingAction[Vector[Boolean], Boolean, Effect],
                 thenMutactions: DBIOAction[Unit, NoStream, Effect.All],
                 elseMutactions: DBIOAction[Unit, NoStream, Effect.All]) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    for {
      exists <- condition
      action <- if (exists.head) thenMutactions else elseMutactions
    } yield action
  }

  def ifThenElseNestedUpsert(condition: SqlStreamingAction[Vector[Boolean], Boolean, Effect],
                             thenMutactions: DBIOAction[Unit, NoStream, Effect.All],
                             elseMutactions: DBIOAction[Unit, NoStream, Effect.All]) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    for {
      exists <- condition
      action <- if (exists.head) thenMutactions else elseMutactions
    } yield action
  }

  def ifThenElseError(condition: SqlStreamingAction[Vector[Boolean], Boolean, Effect],
                      thenMutactions: DBIOAction[Unit, NoStream, Effect],
                      elseError: UserFacingError) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    for {
      exists <- condition
      action <- if (exists.head) thenMutactions else throw elseError
    } yield action
  }
  def triggerFailureWhenExists(query: SQLActionBuilder, table: String, triggerString: String) =
    triggerFailureInternal(query, table, triggerString, notExists = false)
  def triggerFailureWhenNotExists(query: SQLActionBuilder, table: String, triggerString: String) =
    triggerFailureInternal(query, table, triggerString, notExists = true)

  private def triggerFailureInternal(query: SQLActionBuilder, table: String, triggerString: String, notExists: Boolean) = {
    val notValue = if (notExists) s"" else s"not"

    (sql"select case when #$notValue exists ( " ++ query ++ sql" )" ++
      sql"then '' " ++
      sql"""else ("#$schemaName".raise_exception($triggerString))end;""").as[String]
  }

  //endregion

  def createDataItemsImport(mutaction: CreateDataItemsImport): SimpleDBIO[Vector[String]] = {

    SimpleDBIO[Vector[String]] { x =>
      val model         = mutaction.model
      val argsWithIndex = mutaction.args.zipWithIndex

      val nodeResult: Vector[String] = try {
        val columns      = model.scalarNonListFields.map(_.name)
        val escapedKeys  = columns.map(column => s""""$column"""").mkString(",")
        val placeHolders = columns.map(_ => "?").mkString(",")

        val query                         = s"""INSERT INTO "${mutaction.project.id}"."${model.name}" ($escapedKeys) VALUES ($placeHolders)"""
        val itemInsert: PreparedStatement = x.connection.prepareStatement(query)
        val currentTimeStamp              = currentTimeStampUTC

        mutaction.args.foreach { arg =>
          columns.zipWithIndex.foreach { columnAndIndex =>
            val index  = columnAndIndex._2 + 1
            val column = columnAndIndex._1

            arg.raw.asRoot.map.get(column) match {
              case Some(x)                                                => itemInsert.setGcValue(index, x)
              case None if column == "createdAt" || column == "updatedAt" => itemInsert.setTimestamp(index, currentTimeStamp)
              case None                                                   => itemInsert.setNull(index, java.sql.Types.NULL)
            }
          }
          itemInsert.addBatch()
        }

        itemInsert.executeBatch()

        Vector.empty
      } catch {
        case e: java.sql.BatchUpdateException =>
          e.getUpdateCounts.zipWithIndex
            .filter(element => element._1 == Statement.EXECUTE_FAILED)
            .map { failed =>
              val failedId = argsWithIndex.find(_._2 == failed._2).get._1.raw.asRoot.idField.value
              s"Failure inserting ${model.name} with Id: $failedId. Cause: ${removeConnectionInfoFromCause(e.getCause.toString)}"
            }
            .toVector
        case e: Exception => Vector(e.getCause.toString)
      }

      val relayResult: Vector[String] = try {
        val relayQuery                     = s"""INSERT INTO "${mutaction.project.id}"."_RelayId" ("id", "stableModelIdentifier") VALUES (?,?)"""
        val relayInsert: PreparedStatement = x.connection.prepareStatement(relayQuery)

        mutaction.args.foreach { arg =>
          relayInsert.setString(1, arg.raw.asRoot.idField.value)
          relayInsert.setString(2, model.stableIdentifier)
          relayInsert.addBatch()
        }
        relayInsert.executeBatch()

        Vector.empty
      } catch {
        case e: java.sql.BatchUpdateException =>
          e.getUpdateCounts.zipWithIndex
            .filter(element => element._1 == Statement.EXECUTE_FAILED)
            .map { failed =>
              val failedId = argsWithIndex.find(_._2 == failed._2).get._1.raw.asRoot.idField.value
              s"Failure inserting RelayRow with Id: $failedId. Cause: ${removeConnectionInfoFromCause(e.getCause.toString)}"
            }
            .toVector
        case e: Exception => Vector(e.getCause.toString)
      }

      val res = nodeResult ++ relayResult
      if (res.nonEmpty) throw new Exception(res.mkString("-@-"))
      res
    }
  }

  def removeConnectionInfoFromCause(cause: String): String = {
    val connectionSubStringStart = cause.indexOf(": ERROR:")
    cause.substring(connectionSubStringStart + 9)

  }

  def createRelationRowsImport(mutaction: CreateRelationRowsImport): SimpleDBIO[Vector[String]] = {
    val argsWithIndex: Seq[((String, String), Int)] = mutaction.args.zipWithIndex

    SimpleDBIO[Vector[String]] { x =>
      val res = try {
        val query                             = s"""INSERT INTO "${mutaction.project.id}"."${mutaction.relation.relationTableName}" ("id", "A","B") VALUES (?,?,?)"""
        val relationInsert: PreparedStatement = x.connection.prepareStatement(query)
        mutaction.args.foreach { arg =>
          relationInsert.setString(1, Cuid.createCuid())
          relationInsert.setString(2, arg._1)
          relationInsert.setString(3, arg._2)
          relationInsert.addBatch()
        }
        relationInsert.executeBatch()
        Vector.empty
      } catch {
        case e: java.sql.BatchUpdateException =>
          val faileds = e.getUpdateCounts.zipWithIndex

          faileds
            .filter(element => element._1 == Statement.EXECUTE_FAILED)
            .map { failed =>
              val failedA = argsWithIndex.find(_._2 == failed._2).get._1._1
              val failedB = argsWithIndex.find(_._2 == failed._2).get._1._2
              s"Failure inserting into relationtable ${mutaction.relation.relationTableName} with ids $failedA and $failedB. Cause: ${removeConnectionInfoFromCause(
                e.getCause.toString)}"
            }
            .toVector
        case e: Exception =>
          println(e.getMessage)
          Vector(e.getMessage)
      }

      if (res.nonEmpty) throw new Exception(res.mkString("-@-"))
      res
    }
  }

  def pushScalarListsImport(mutaction: PushScalarListsImport) = {

    val projectId = mutaction.project.id
    val tableName = mutaction.tableName
    val nodeId    = mutaction.id

    val idQuery =
      sql"""Select "case" from (
            Select max("position"),
            CASE WHEN max("position") IS NULL THEN 1000
            ELSE max("position") +1000
            END
            FROM "#$schemaName"."#$tableName"
            WHERE "nodeId" = $nodeId
            ) as "ALIAS"
      """.as[Int]

    def pushQuery(baseLine: Int) = SimpleDBIO[Vector[String]] { x =>
      val argsWithIndex = mutaction.args.values.zipWithIndex
      val rowResult: Vector[String] = try {
        val query                         = s"""insert into "$schemaName"."$tableName" ("nodeId", "position", "value") values (?, $baseLine + ? , ?)"""
        val insertRows: PreparedStatement = x.connection.prepareStatement(query)

        argsWithIndex.foreach { argWithIndex =>
          insertRows.setString(1, nodeId)
          insertRows.setInt(2, argWithIndex._2 * 1000)
          insertRows.setGcValue(3, argWithIndex._1)
          insertRows.addBatch()
        }
        insertRows.executeBatch()

        Vector.empty
      } catch {
        case e: java.sql.BatchUpdateException =>
          e.getUpdateCounts.zipWithIndex
            .filter(element => element._1 == Statement.EXECUTE_FAILED)
            .map { failed =>
              val failedValue: GCValue = argsWithIndex.find(_._2 == failed._2).get._1
              s"Failure inserting into listTable $tableName for the id $nodeId for value ${GCValueExtractor
                .fromGCValue(failedValue)}. Cause: ${removeConnectionInfoFromCause(e.getCause.toString)}"
            }
            .toVector

        case e: Exception =>
          println(e.getMessage)
          Vector(e.getMessage)
      }

      if (rowResult.nonEmpty) throw new Exception(rowResult.mkString("-@-"))
      rowResult
    }

    import scala.concurrent.ExecutionContext.Implicits.global

    for {
      nodeIds <- idQuery
      action  <- pushQuery(nodeIds.head)
    } yield action
  }

  private val dbioUnit = DBIO.successful(())
}
