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
import slick.jdbc.PostgresProfile.api._
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

      val generatedKeys = itemInsert.getGeneratedKeys
      generatedKeys.next()
      val field = path.lastModel.idField_!
      CreateDataItemResult(generatedKeys.getGcValue(field.dbName, field.typeIdentifier))
    }
  }

  def createRelayRow(path: Path): SqlStreamingAction[Vector[Int], Int, Effect]#ResultAction[Int, NoStream, Effect] = {
    val where = path.lastCreateWhere_!
    sql"""INSERT INTO "#$schemaName"."_RelayId" ("id", "stableModelIdentifier") VALUES (${where.fieldValue}, ${where.model.stableIdentifier})""".asUpdate
  }

  def createRelationRowByPath(path: Path): SqlAction[Int, NoStream, Effect] = {
    val relation = path.lastRelation_!
    val childWhere = path.lastEdge_! match {
      case _: ModelEdge   => sys.error("Needs to be a node edge.")
      case edge: NodeEdge => edge.childWhere
    }

    if (relation.isInlineRelation) {
      val inlineManifestation = relation.inlineManifestation.get
      val referencingColumn   = inlineManifestation.referencingColumn
      val tableName           = relation.relationTableName
      val otherModel = if (inlineManifestation.inTableOfModelId == relation.modelAId) {
        schema.getModelById_!(relation.modelBId)
      } else {
        schema.getModelById_!(relation.modelAId)
      }
      val childWhereCondition = sql"""where "#$schemaName"."#${childWhere.model.dbName}"."#${childWhere.field.dbName}" = ${childWhere.fieldValue}"""
      val otherWhereCondition = sql"""where "#$schemaName"."#${path.removeLastEdge.lastModel.dbName}"."#${path.removeLastEdge.lastModel.dbNameOfIdField_!}" in (""" ++ pathQueryForLastChild(
        path.removeLastEdge) ++ sql")"
      val selectIdOfChild = sql"""select "#${childWhere.model.dbNameOfIdField_!}" as id from "#$schemaName"."#${childWhere.model.dbName}" """ ++ childWhereCondition
      val selectIdOfOther = sql"""select "#${otherModel.dbNameOfIdField_!}" as id from "#$schemaName"."#${otherModel.dbName}" """ ++ otherWhereCondition

      val rowToUpdateCondition = if (relation.isSameModelRelation) {
        if (path.lastEdge_!.childField.get.relationSide.get == RelationSide.A) {
          childWhereCondition
        } else {
          otherWhereCondition
        }
      } else {
        if (inlineManifestation.inTableOfModelId == childWhere.model.name) {
          childWhereCondition
        } else {
          otherWhereCondition
        }
      }

      val nodeToLinkToCondition = if (relation.isSameModelRelation) {
        if (path.lastEdge_!.childField.get.relationSide.get == RelationSide.A) {
          selectIdOfOther
        } else {
          selectIdOfChild
        }
      } else {
        if (inlineManifestation.inTableOfModelId == childWhere.model.name) {
          selectIdOfOther
        } else {
          selectIdOfChild
        }
      }

      (sql"""update "#$schemaName"."#$tableName" """ ++
        sql"""set "#$referencingColumn" = subquery.id""" ++
        sql"""from (""" ++ nodeToLinkToCondition ++ sql""") as subquery""" ++
        rowToUpdateCondition).asUpdate

    } else if (relation.hasManifestation) {
      val nodeEdge        = path.lastEdge_!.asInstanceOf[NodeEdge]
      val parentModel     = nodeEdge.parent
      val childModel      = nodeEdge.child
      val manifestation   = relation.manifestation.get.asInstanceOf[RelationTableManifestation]
      val columnForParent = if (parentModel.name == relation.modelAId) manifestation.modelAColumn else manifestation.modelBColumn
      val columnForChild  = if (childModel.name == relation.modelAId) manifestation.modelAColumn else manifestation.modelBColumn

      (sql"""insert into "#$schemaName"."#${path.lastRelation_!.relationTableName}" ("#$columnForParent", "#$columnForChild")""" ++
        sql"""Select """ ++ pathQueryForLastChild(path.removeLastEdge) ++ sql"," ++
        sql""" "#${childWhere.model.dbNameOfIdField_!}" FROM "#$schemaName"."#${childWhere.model.dbName}" where "#${childWhere.field.dbName}" = ${childWhere.fieldValue}""").asUpdate
    } else {
      val relationId = Cuid.createCuid()
      (sql"""insert into "#$schemaName"."#${path.lastRelation_!.relationTableName}" """ ++
        sql"""("id", "#${path.columnForParentSideOfLastEdge(schema)}", "#${path.columnForChildSideOfLastEdge(schema)}")""" ++
        sql"""Select '#$relationId',""" ++ pathQueryForLastChild(path.removeLastEdge) ++ sql""","#${childWhere.model.dbNameOfIdField_!}" """ ++
        sql"""FROM "#$schemaName"."#${childWhere.model.dbName}" where "#${childWhere.field.dbName}" = ${childWhere.fieldValue}""").asUpdate
    }
//    https://stackoverflow.com/questions/1109061/insert-on-duplicate-update-in-postgresql
//    ++
//      sql"on conflict (id )  key update #$databaseName.#${path.lastRelation_!.relationTableName}.id = #$databaseName.#${path.lastRelation_!.relationTableName}.id").asUpdate
  }

  //endregion

  //region UPDATE

  def updateDataItems(model: Model, args: PrismaArgs, whereFilter: Option[DataItemFilterCollection]) = {
    val updateValues = combineByComma(args.raw.asRoot.map.map { case (k, v) => escapeKey(model.getFieldByName_!(k).dbName) ++ sql" = $v" })

    if (updateValues.isDefined) {
      (sql"""UPDATE "#$schemaName"."#${model.dbName}"""" ++ sql"SET " ++ addUpdatedDateTime(model, updateValues) ++
        whereFilterAppendix(schemaName, model.dbName, whereFilter)).asUpdate
    } else {
      DBIOAction.successful(())
    }
  }

  def updateDataItemByPath(path: Path, updateArgs: PrismaArgs) = {
    val model        = path.lastModel
    val updateValues = combineByComma(updateArgs.raw.asRoot.map.map { case (k, v) => escapeKey(model.getFieldByName_!(k).dbName) ++ sql" = $v" })
    def fromEdge(edge: Edge) = edge match {
      case edge: NodeEdge => sql""" "#${path.columnForChildSideOfLastEdge(schema)}"""" ++ idFromWhereEquals(edge.childWhere) ++ sql" AND "
      case _: ModelEdge   => sql""
    }

    val baseQuery = sql"""UPDATE "#$schemaName"."#${model.dbName}" SET """ ++ addUpdatedDateTime(model, updateValues) ++ sql"""WHERE "#${path.lastModel.dbNameOfIdField_!}" ="""

    if (updateArgs.raw.asRoot.map.isEmpty) {
      DBIOAction.successful(())
    } else {

      val query = path.lastEdge match {
        case Some(edge) =>
          baseQuery ++ sql"""(SELECT "#${path.columnForChildSideOfLastEdge(schema)}" """ ++
            sql"""FROM "#$schemaName"."#${path.lastRelation_!.relationTableName}"""" ++
            sql"WHERE" ++ fromEdge(edge) ++ sql""""#${path.columnForParentSideOfLastEdge(schema)}" = """ ++ pathQueryForLastParent(path) ++ sql")"
        case None => baseQuery ++ idFromWhere(path.root)
      }
      query.asUpdate
    }
  }

  //endregion

  //region UPSERT
  private def addUpdatedDateTime(model: Model, updateValues: Option[SQLActionBuilder]): Option[SQLActionBuilder] = {
    model.updatedAtField match {
      case Some(updatedAtField) =>
        val today              = new Date()
        val exactlyNow         = new DateTime(today).withZone(DateTimeZone.UTC)
        val currentDateGCValue = DateTimeGCValue(exactlyNow)
        val updatedAt          = sql""""#${updatedAtField.dbName}" = $currentDateGCValue """
        combineByComma(updateValues ++ List(updatedAt))
      case None =>
        updateValues
    }
  }

  def upsert(
      createPath: Path,
      updatePath: Path,
      createArgs: PrismaArgs,
      updateArgs: PrismaArgs,
      create: slick.dbio.DBIOAction[Unit, slick.dbio.NoStream, slick.dbio.Effect with slick.dbio.Effect.All],
      update: slick.dbio.DBIOAction[Unit, slick.dbio.NoStream, slick.dbio.Effect with slick.dbio.Effect.All]
  ) = {
    val model = updatePath.lastModel
    val query = sql"""select exists ( SELECT "#${model.dbNameOfIdField_!}" FROM "#$schemaName"."#${model.dbName}" WHERE "#${model.dbNameOfIdField_!}" = """ ++
      pathQueryForLastChild(updatePath) ++ sql")"
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
        case edge: NodeEdge => sql" #${edge.child.dbNameOfIdField_!}" ++ idFromWhereEquals(edge.childWhere) ++ sql" AND "
        case _: ModelEdge   => sql""
      }
      val model = updatePath.lastModel
      sql"""select EXISTS (
            select "#${model.dbNameOfIdField_!}" from "#$schemaName"."#${updatePath.lastModel.dbName}"
            where""" ++ nodeSelector(updatePath.lastEdge_!) ++
        sql""" "#${model.dbNameOfIdField_!}" IN""" ++ pathQueryThatUsesWholePath(updatePath) ++ sql")"
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
    (sql"""DELETE FROM "#$schemaName"."#${model.dbName}"""" ++ whereFilterAppendix(schemaName, model.dbName, whereFilter)).asUpdate
  }

  def deleteRelayIds(model: Model, whereFilter: Option[DataItemFilterCollection]) = {
    (sql"""DELETE FROM "#$schemaName"."_RelayId" WHERE "id" IN ( SELECT "#${model.dbNameOfIdField_!}" FROM "#$schemaName"."#${model.dbName}"""" ++ whereFilterAppendix(
      schemaName,
      model.dbName,
      whereFilter) ++ sql")").asUpdate
  }

  def deleteDataItem(path: Path) =
    (sql"""DELETE FROM "#$schemaName"."#${path.lastModel.dbName}" WHERE "#${path.lastModel.dbNameOfIdField_!}" = """ ++ pathQueryForLastChild(path)).asUpdate

  def deleteRelayRow(path: Path) =
    (sql"""DELETE FROM "#$schemaName"."_RelayId" WHERE "id" = """ ++ pathQueryForLastChild(path)).asUpdate

  def deleteRelationRowByParent(path: Path): DBIO[Unit] = {
    val relation      = path.lastRelation_!
    val relationTable = path.lastRelation_!.relationTableName
    val action = relation.inlineManifestation match {
      case Some(manifestation) =>
        (sql"""UPDATE "#$schemaName"."#$relationTable" """ ++
          sql"""SET "#${manifestation.referencingColumn}" = NULL""" ++
          sql"""WHERE "#${path.columnForParentSideOfLastEdge(schema)}" IN """ ++ pathQueryThatUsesWholePath(path.removeLastEdge)).asUpdate
      case None =>
        (sql"""DELETE FROM "#$schemaName"."#$relationTable" WHERE "#${path.columnForParentSideOfLastEdge(schema)}" = """ ++ pathQueryForLastParent(path)).asUpdate
    }
    action.andThen(dbioUnit)
  }

  def deleteRelationRowByChildWithWhere(path: Path): DBIO[Unit] = {
    val relation      = path.lastRelation_!
    val relationTable = path.lastRelation_!.relationTableName
    val where = path.lastEdge_! match {
      case _: ModelEdge   => sys.error("Should be a node Edge")
      case edge: NodeEdge => edge.childWhere
    }
    val action = relation.inlineManifestation match {
      case Some(manifestation) =>
        (sql"""UPDATE "#$schemaName"."#$relationTable" """ ++
          sql"""SET "#${manifestation.referencingColumn}" = NULL""" ++
          sql"""WHERE "#${path.columnForChildSideOfLastEdge(schema)}" IN """ ++ pathQueryThatUsesWholePath(path) ++
          sql""" AND "#${path.columnForParentSideOfLastEdge(schema)}" IN """ ++ pathQueryThatUsesWholePath(path.removeLastEdge)).asUpdate
      case None =>
        (sql"""DELETE FROM "#$schemaName"."#$relationTable" WHERE "#${path.columnForChildSideOfLastEdge(schema)}"""" ++ idFromWhereEquals(where)).asUpdate
    }
    action.andThen(dbioUnit)
  }

  def deleteRelationRowByParentAndChild(path: Path) = {
    val relation = path.lastRelation_!
    relation.inlineManifestation match {
      case Some(manifestation) =>
        (sql"""UPDATE "#$schemaName"."#${path.lastRelation_!.relationTableName}"  """ ++
          sql"""SET "#${manifestation.referencingColumn}" = NULL """ ++
          sql"""WHERE "#${path.columnForChildSideOfLastEdge(schema)}" = """ ++ pathQueryForLastChild(path) ++
          sql""" AND "#${path.columnForParentSideOfLastEdge(schema)}" = """ ++ pathQueryForLastParent(path)).asUpdate.andThen(dbioUnit)
      case _ =>
        (sql"""DELETE FROM "#$schemaName"."#${path.lastRelation_!.relationTableName}" """ ++
          sql"""WHERE "#${path.columnForChildSideOfLastEdge(schema)}" = """ ++ pathQueryForLastChild(path) ++
          sql""" AND "#${path.columnForParentSideOfLastEdge(schema)}" = """ ++ pathQueryForLastParent(path)).asUpdate.andThen(dbioUnit)
    }
  }

  def cascadingDeleteChildActions(path: Path) = {
    val deleteRelayIds = (sql"""DELETE FROM "#$schemaName"."_RelayId" WHERE "id" IN (""" ++ pathQueryForLastChild(path) ++ sql")").asUpdate
    val model          = path.lastModel
    val deleteDataItems = (sql"""DELETE FROM "#$schemaName"."#${model.dbName}" WHERE "#${model.dbNameOfIdField_!}" IN ("""
      ++ pathQueryForLastChild(path) ++ sql")").asUpdate

    DBIO.seq(deleteRelayIds, deleteDataItems)
  }

  //endregion

  //region SCALAR LISTS
  def setScalarList(path: Path, listFieldMap: Vector[(String, ListGCValue)]) = {
    val model = path.lastModel
    val idQuery = (sql"""SELECT "#${model.dbNameOfIdField_!}" FROM "#$schemaName"."#${model.dbName}" WHERE "#${model.dbNameOfIdField_!}" = """ ++
      pathQueryForLastChild(path)).as[String]
    if (listFieldMap.isEmpty) DBIOAction.successful(()) else setManyScalarListHelper(path.lastModel, listFieldMap, idQuery)
  }

  def setManyScalarLists(model: Model, listFieldMap: Vector[(String, ListGCValue)], whereFilter: Option[DataItemFilterCollection]) = {
    val idQuery =
      (sql"""SELECT "#${model.dbNameOfIdField_!}" FROM "#$schemaName"."#${model.dbName}"""" ++ whereFilterAppendix(schemaName, model.dbName, whereFilter))
        .as[String]
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
              val dbNameOfField                    = model.getFieldByName_!(fieldName).dbName
              val wipe                             = s"""DELETE  FROM "$schemaName"."${model.dbName}_$dbNameOfField" $whereString"""
              val wipeOldValues: PreparedStatement = x.connection.prepareStatement(wipe)
              wipeOldValues.executeUpdate()

              val insert                             = s"""INSERT INTO "$schemaName"."${model.dbName}_$dbNameOfField" ("nodeId", "position", "value") VALUES (?,?,?)"""
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
      sql"""(SELECT "#${where.model.dbNameOfIdField_!}" FROM (SELECT * FROM "#$schemaName"."#${where.model.dbName}") IDFROMWHERE WHERE "#${where.field.dbName}" = ${where.fieldValue})"""
    }
  }

  def idFromWhereEquals(where: NodeSelector): SQLActionBuilder = sql" = " ++ idFromWhere(where)

  def idFromWherePath(where: NodeSelector): SQLActionBuilder = {
    sql"""(SELECT "#${where.model.dbNameOfIdField_!}" FROM (SELECT  * From "#$schemaName"."#${where.model.dbName}") IDFROMWHEREPATH WHERE "#${where.field.dbName}" = ${where.fieldValue})"""
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
          case edge: NodeEdge => sql""" "#${edge.columnForChildRelationSide(schema)}"""" ++ idFromWhereEquals(edge.childWhere) ++ sql" AND "
          case _: ModelEdge   => sql""
        }

        sql"""(SELECT "#${last.columnForChildRelationSide(schema)}"""" ++
          sql""" FROM (SELECT * FROM "#$schemaName"."#${last.relation.relationTableName}") PATHQUERY""" ++
          sql" WHERE " ++ childWhere ++ sql""""#${last.columnForParentRelationSide(schema)}" IN (""" ++ pathQueryForLastParent(path) ++ sql"))"
    }
  }

  def whereFailureTrigger(where: NodeSelector, causeString: String) = {
    val table = where.model.dbName
    val query =
      sql"""(SELECT "#${where.model.dbNameOfIdField_!}" FROM "#$schemaName"."#${table}" WHEREFAILURETRIGGER WHERE "#${where.field.dbName}" = ${where.fieldValue})"""

    triggerFailureWhenNotExists(query, table, causeString)
  }

  def connectionFailureTrigger(path: Path, causeString: String) = {
    val table = path.lastRelation.get.relationTableName

    val lastChildWhere = path.lastEdge_! match {
      case edge: NodeEdge => sql""" "#${path.columnForChildSideOfLastEdge(schema)}"""" ++ idFromWhereEquals(edge.childWhere) ++ sql" AND "
      case _: ModelEdge   => sql""" "#${path.columnForChildSideOfLastEdge(schema)}" IS NOT NULL AND """
    }

    val query =
      sql"""SELECT * FROM "#$schemaName"."#$table" CONNECTIONFAILURETRIGGERPATH""" ++
        sql"WHERE" ++ lastChildWhere ++ sql""""#${path.columnForParentSideOfLastEdge(schema)}" = """ ++ pathQueryForLastParent(path)

    triggerFailureWhenNotExists(query, table, causeString)
  }

  def oldParentFailureTriggerForRequiredRelations(relation: Relation,
                                                  where: NodeSelector,
                                                  childSide: RelationSide.Value,
                                                  triggerString: String): slick.sql.SqlStreamingAction[Vector[String], String, slick.dbio.Effect] = {
    val table       = relation.relationTableName
    val column      = relation.columnForRelationSide(childSide)
    val otherColumn = relation.columnForRelationSide(RelationSide.opposite(childSide))
    val query = sql"""SELECT * FROM "#$schemaName"."#$table" OLDPARENTFAILURETRIGGER WHERE "#$column" """ ++
      idFromWhereEquals(where) ++ sql""" AND "#$otherColumn" IS NOT NULL """

    triggerFailureWhenExists(query, table, triggerString)
  }

  def oldParentFailureTrigger(path: Path, triggerString: String) = {
    val table = path.lastRelation_!.relationTableName
    val query = sql"""SELECT * FROM "#$schemaName"."#$table" OLDPARENTPATHFAILURETRIGGER WHERE "#${path.columnForChildSideOfLastEdge(schema)}" IN (""" ++
      pathQueryForLastChild(path) ++ sql")"
    triggerFailureWhenExists(query, table, triggerString)
  }

  def oldParentFailureTriggerByField(path: Path, field: Field, triggerString: String) = {
    val relation       = field.relation.get
    val table          = relation.relationTableName
    val oppositeColumn = relation.columnForRelationSide(field.oppositeRelationSide.get)
    val column         = relation.columnForRelationSide(field.relationSide.get)
    val query = sql"""SELECT * FROM "#$schemaName"."#$table" OLDPARENTPATHFAILURETRIGGERBYFIELD""" ++
      sql"""WHERE "#$oppositeColumn" IN (""" ++ pathQueryForLastChild(path) ++ sql") " ++
      sql"""AND "#$column" IS NOT NULL"""
    triggerFailureWhenExists(query, table, triggerString)
  }

  def oldParentFailureTriggerByFieldAndFilter(model: Model, whereFilter: Option[DataItemFilterCollection], field: Field, causeString: String) = {
    val relation       = field.relation.get
    val table          = relation.relationTableName
    val column         = relation.columnForRelationSide(field.oppositeRelationSide.get)
    val oppositeColumn = relation.columnForRelationSide(field.relationSide.get)

    val query =
      sql"""SELECT * FROM "#$schemaName"."#$table" OLDPARENTPATHFAILURETRIGGERBYFIELDANDFILTER""" ++
        sql"""WHERE "#$oppositeColumn" IS NOT NULL """ ++
        sql"""AND "#$column" IN (""" ++
        sql"""SELECT "#${model.dbNameOfIdField_!}" FROM "#$schemaName"."#${model.dbName}" """ ++
        whereFilterAppendix(schemaName, model.dbName, whereFilter) ++ sql""")"""
    triggerFailureWhenExists(query, table, causeString)
  }

  def oldChildFailureTrigger(path: Path, triggerString: String) = {
    val table = path.lastRelation_!.relationTableName
    val query = sql"""SELECT * FROM "#$schemaName"."#$table" OLDCHILDPATHFAILURETRIGGER""" ++
      sql"""WHERE "#${path.columnForParentSideOfLastEdge(schema)}" IN (""" ++ pathQueryForLastParent(path) ++ sql") " ++
      sql"""AND "#${path.columnForChildSideOfLastEdge(schema)}" IS NOT NULL """
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

    SimpleDBIO[Vector[String]] { jdbcActionContext =>
      val model         = mutaction.model
      val argsWithIndex = mutaction.args.zipWithIndex

      val nodeResult: Vector[String] = try {
        val columns      = model.scalarNonListFields.map(_.dbName)
        val escapedKeys  = columns.map(column => s""""$column"""").mkString(",")
        val placeHolders = columns.map(_ => "?").mkString(",")

        val query                         = s"""INSERT INTO "$schemaName"."${model.dbName}" ($escapedKeys) VALUES ($placeHolders)"""
        val itemInsert: PreparedStatement = jdbcActionContext.connection.prepareStatement(query)
        val currentTimeStamp              = currentTimeStampUTC

        mutaction.args.foreach { arg =>
          val argsAsRoot = arg.raw.asRoot
          model.scalarNonListFields.zipWithIndex.foreach {
            case (field, index) =>
              argsAsRoot.map.get(field.name) match {
                case Some(NullGCValue) if field.name == "createdAt" || field.name == "updatedAt" => itemInsert.setTimestamp(index + 1, currentTimeStamp)
                case Some(gCValue)                                                               => itemInsert.setGcValue(index + 1, gCValue)
                case None if field.name == "createdAt" || field.name == "updatedAt"              => itemInsert.setTimestamp(index + 1, currentTimeStamp)
                case None                                                                        => itemInsert.setNull(index + 1, java.sql.Types.NULL)
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
              s"Failure inserting ${model.dbName} with Id: $failedId. Cause: ${removeConnectionInfoFromCause(e.getCause.toString)}"
            }
            .toVector
        case e: Exception => Vector(e.getCause.toString)
      }

      val relayResult: Vector[String] = try {
        val relayQuery                     = s"""INSERT INTO "$schemaName"."_RelayId" ("id", "stableModelIdentifier") VALUES (?,?)"""
        val relayInsert: PreparedStatement = jdbcActionContext.connection.prepareStatement(relayQuery)

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
        val query                             = s"""INSERT INTO "$schemaName"."${mutaction.relation.relationTableName}" ("id", "A","B") VALUES (?,?,?)"""
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
