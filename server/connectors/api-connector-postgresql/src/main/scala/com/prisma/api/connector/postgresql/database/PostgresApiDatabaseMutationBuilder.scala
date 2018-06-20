package com.prisma.api.connector.postgresql.database

import java.sql.{PreparedStatement, Statement}
import java.util.Date

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.JdbcExtensions._
import com.prisma.api.connector.postgresql.database.PostgresSlickExtensions._
import com.prisma.api.schema.UserFacingError
import com.prisma.gc_values.{GCValue, ListGCValue, NullGCValue, _}
import com.prisma.shared.models._
import com.prisma.slick.NewJdbcExtensions._
import cool.graph.cuid.Cuid
import org.joda.time.{DateTime, DateTimeZone}
import org.jooq._
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DSL._
import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{PositionedParameters, SQLActionBuilder}
import slick.sql.SqlStreamingAction

import scala.collection.JavaConverters._

case class PostgresApiDatabaseMutationBuilder(schemaName: String) {
  import JooqQueryBuilders._

  val sql = DSL.using(SQLDialect.POSTGRES_9_5, new Settings().withRenderFormatted(true))

  // region CREATE

  def createDataItem(model: Model, args: PrismaArgs): DBIO[CreateDataItemResult] = {

    SimpleDBIO[CreateDataItemResult] { x =>
      val argsAsRoot = args.raw.asRoot
      val fields     = model.fields.filter(field => argsAsRoot.hasArgFor(field.name))
      val columns    = fields.map(_.dbName)

      lazy val queryString: String = {
        val generatedFields = columns.map(fieldName => field(name(schemaName, model.dbName, fieldName)))

        sql
          .insertInto(table(name(schemaName, model.dbName)))
          .columns(generatedFields: _*)
          .values(columns.map(_ => placeHolder): _*)
          .getSQL
      }

      val itemInsert: PreparedStatement = x.connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS)

      fields.map(_.name).zipWithIndex.foreach {
        case (column, index) =>
          argsAsRoot.map.get(column) match {
            case Some(NullGCValue) if column == createdAtField || column == updatedAtField => itemInsert.setTimestamp(index + 1, currentTimeStampUTC)
            case Some(gCValue)                                                             => itemInsert.setGcValue(index + 1, gCValue)
            case None if column == createdAtField || column == updatedAtField              => itemInsert.setTimestamp(index + 1, currentTimeStampUTC)
            case None                                                                      => itemInsert.setNull(index + 1, java.sql.Types.NULL)
          }
      }
      itemInsert.execute()

      val generatedKeys = itemInsert.getGeneratedKeys
      generatedKeys.next()
      CreateDataItemResult(generatedKeys.getId(model))
    }
  }

  def createRelayRow(where: NodeSelector): DBIO[_] = {
    SimpleDBIO[Boolean] { x =>
      lazy val queryString: String = {
        sql
          .insertInto(table(name(schemaName, relayTableName)))
          .columns(field(name(schemaName, relayTableName, "id")), field(name(schemaName, relayTableName, "stableModelIdentifier")))
          .values(placeHolder, placeHolder)
          .getSQL
      }

      val statement: PreparedStatement = x.connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS)
      statement.setGcValue(1, where.fieldGCValue)
      statement.setString(2, where.model.stableIdentifier)

      statement.execute()
    }
  }

  def createRelationRowByPath(relationField: RelationField, parentId: IdGCValue, childId: IdGCValue): DBIO[_] = {
    val relation = relationField.relation

    if (relation.isInlineRelation) {
      ???

      //Fixme

//      val inlineManifestation = relation.inlineManifestation.get
//      val referencingColumn   = inlineManifestation.referencingColumn
//      val tableName           = relation.relationTableName
//      val otherModel = if (inlineManifestation.inTableOfModelId == relation.modelAName) {
//        relation.modelB
//      } else {
//        relation.modelA
//      }
//      val childWhereCondition = sql"""where "#$schemaName"."#${childWhere.model.dbName}"."#${childWhere.field.dbName}" = ${childWhere.fieldGCValue}"""
//      val otherWhereCondition = sql"""where "#$schemaName"."#${path.removeLastEdge.lastModel.dbName}"."#${path.removeLastEdge.lastModel.dbNameOfIdField_!}" in (""" ++ pathQueryForLastChild(
//        path.removeLastEdge) ++ sql")"
//      val selectIdOfChild = sql"""select "#${childWhere.model.dbNameOfIdField_!}" as id from "#$schemaName"."#${childWhere.model.dbName}" """ ++ childWhereCondition
//      val selectIdOfOther = sql"""select "#${otherModel.dbNameOfIdField_!}" as id from "#$schemaName"."#${otherModel.dbName}" """ ++ otherWhereCondition
//
//      val rowToUpdateCondition = if (relation.isSameModelRelation) {
//        if (path.lastEdge_!.childField.relationSide == RelationSide.A) {
//          childWhereCondition
//        } else {
//          otherWhereCondition
//        }
//      } else {
//        if (inlineManifestation.inTableOfModelId == childWhere.model.name) {
//          childWhereCondition
//        } else {
//          otherWhereCondition
//        }
//      }
//
//      val nodeToLinkToCondition = if (relation.isSameModelRelation) {
//        if (path.lastEdge_!.childField.relationSide == RelationSide.A) {
//          selectIdOfOther
//        } else {
//          selectIdOfChild
//        }
//      } else {
//        if (inlineManifestation.inTableOfModelId == childWhere.model.name) {
//          selectIdOfOther
//        } else {
//          selectIdOfChild
//        }
//      }
//
//      (sql"""update "#$schemaName"."#$tableName" """ ++
//        sql"""set "#$referencingColumn" = subquery.id""" ++
//        sql"""from (""" ++ nodeToLinkToCondition ++ sql""") as subquery""" ++
//        rowToUpdateCondition).asUpdate

    } else if (relation.hasManifestation) {
      SimpleDBIO[Boolean] { x =>
        lazy val queryString: String = {

          val relationTable = table(name(schemaName, relation.relationTableName))

          sql
            .insertInto(relationTable)
            .columns(
              field(name(schemaName, relation.relationTableName, relationField.relationSide.toString)),
              field(name(schemaName, relation.relationTableName, relationField.oppositeRelationSide.toString))
            )
            .values(placeHolder, placeHolder)
            .getSQL
        }

        val statement: PreparedStatement = x.connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS)
        statement.setGcValue(1, parentId)
        statement.setGcValue(2, childId)

        statement.execute()
      }
    } else {
      SimpleDBIO[Boolean] { x =>
        lazy val queryString: String = {
          val relationTable = table(name(schemaName, relation.relationTableName))

          sql
            .insertInto(relationTable)
            .columns(
              field(name(schemaName, relation.relationTableName, "id")),
              field(name(schemaName, relation.relationTableName, relationField.relationSide.toString)),
              field(name(schemaName, relation.relationTableName, relationField.oppositeRelationSide.toString))
            )
            .values(placeHolder, placeHolder, placeHolder)
            .getSQL
        }

        val statement: PreparedStatement = x.connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS)
        statement.setString(1, Cuid.createCuid())
        statement.setGcValue(2, parentId)
        statement.setGcValue(3, childId)

        statement.execute()
      }
    }
  }

  //endregion

  //region UPDATE

  def updateDataItems(model: Model, args: PrismaArgs, whereFilter: Option[Filter]): DBIO[_] = {
    val map = args.raw.asRoot.map
    if (map.nonEmpty) {
      SimpleDBIO { ctx =>
        val aliasedTable = table(name(schemaName, model.dbName)).as(topLevelAlias)
        val condition    = JooqWhereClauseBuilder(schemaName).buildWhereClause(whereFilter).getOrElse(trueCondition())

        val base = sql.update(aliasedTable)

        //https://www.postgresql.org/message-id/20170719174507.GA19616%40telsasoft.com
        lazy val queryString: String = if (map.size > 1) {

          val fields = map.map { case (k, _) => field(model.getFieldByName_!(k).dbName) }.toList.asJava
          val values = map.map(_ => placeHolder).toList.asJava

          base
            .set(row(fields), row(values))
            .where(condition)
            .getSQL

        } else {
          val fieldDef = map.map { case (k, _) => field(model.getFieldByName_!(k).dbName) }.head
          val value    = map.map(_ => placeHolder).head

          base
            .set(fieldDef, value)
            .where(condition)
            .getSQL
        }

        val ps = ctx.connection.prepareStatement(queryString)
        val pp = new PositionedParameters(ps)
        map.foreach { case (_, v) => pp.setGcValue(v) }
        whereFilter.foreach(filter => JooqSetParams.setParams(pp, filter))
        ps.executeUpdate()
      }
    } else {
      dbioUnit
    }
  }

  def updateDataItemByPath(path: Path, updateArgs: PrismaArgs) = {
    val model        = path.lastModel
    val updateValues = combineByComma(updateArgs.raw.asRoot.map.map { case (k, v) => escapeKey(model.getFieldByName_!(k).dbName) ++ sql" = $v" })
    def fromEdge(edge: Edge) = edge match {
      case edge: NodeEdge => sql""" "#${path.columnForChildSideOfLastEdge}"""" ++ idFromWhereEquals(edge.childWhere) ++ sql" AND "
      case _: ModelEdge   => sql""
    }

    val baseQuery = sql"""UPDATE "#$schemaName"."#${model.dbName}" SET """ ++ addUpdatedDateTime(model, updateValues) ++ sql"""WHERE "#${path.lastModel.dbNameOfIdField_!}" ="""

    if (updateArgs.raw.asRoot.map.isEmpty) {
      DBIOAction.successful(())
    } else {

      val query = path.lastEdge match {
        case Some(edge) =>
          baseQuery ++ sql"""(SELECT "#${path.columnForChildSideOfLastEdge}" """ ++
            sql"""FROM "#$schemaName"."#${path.lastRelation_!.relationTableName}"""" ++
            sql"WHERE" ++ fromEdge(edge) ++ sql""""#${path.columnForParentSideOfLastEdge}" = """ ++ pathQueryForLastParent(path) ++ sql")"
        case None => baseQuery ++ idFromWhere(path.root)
      }
      query.asUpdate
    }
  }

  //endregion

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

  //region UPSERT

  def upsert(
      createPath: Path,
      updatePath: Path,
      createArgs: PrismaArgs,
      updateArgs: PrismaArgs,
      create: slick.dbio.DBIOAction[Unit, slick.dbio.NoStream, slick.dbio.Effect with slick.dbio.Effect.All],
      update: slick.dbio.DBIOAction[Unit, slick.dbio.NoStream, slick.dbio.Effect with slick.dbio.Effect.All],
      createNested: Vector[DBIOAction[Any, NoStream, Effect.All]],
      updateNested: Vector[DBIOAction[Any, NoStream, Effect.All]]
  ) = {
    val model = updatePath.lastModel
    val query = sql"""select exists ( SELECT "#${model.dbNameOfIdField_!}" FROM "#$schemaName"."#${model.dbName}" WHERE "#${model.dbNameOfIdField_!}" = """ ++
      pathQueryForLastChild(updatePath) ++ sql")"
    val condition        = query.as[Boolean]
    val allCreateActions = Vector(createDataItem(model, createArgs), createRelayRow(createPath.lastCreateWhere_!), create) ++ createNested
    val qCreate          = DBIOAction.seq(allCreateActions: _*)
    // update first sets the lists, then updates the item
    val allUpdateActions = update +: updateNested :+ updateDataItemByPath(updatePath, updateArgs)
    val qUpdate          = DBIOAction.seq(allUpdateActions: _*)

    ifThenElse(condition, qUpdate, qCreate)
  }

  def upsertIfInRelationWith(
      createPath: Path,
      updatePath: Path,
      createArgs: PrismaArgs,
      updateArgs: PrismaArgs,
      scalarListCreate: DBIO[_],
      scalarListUpdate: DBIO[_],
      createCheck: DBIO[_],
      createNested: Vector[DBIOAction[Any, NoStream, Effect.All]],
      updateNested: Vector[DBIOAction[Any, NoStream, Effect.All]]
  ) = {

//    def existsNodeIsInRelationshipWith = {
//      def nodeSelector(last: Edge) = last match {
//        case edge: NodeEdge => sql" #${edge.child.dbNameOfIdField_!}" ++ idFromWhereEquals(edge.childWhere) ++ sql" AND "
//        case _: ModelEdge   => sql""
//      }
//      val model = updatePath.lastModel
//      sql"""select EXISTS (
//            select "#${model.dbNameOfIdField_!}" from "#$schemaName"."#${updatePath.lastModel.dbName}"
//            where""" ++ nodeSelector(updatePath.lastEdge_!) ++
//        sql""" "#${model.dbNameOfIdField_!}" IN""" ++ pathQueryThatUsesWholePath(updatePath) ++ sql")"
//    }
//
//    val condition = existsNodeIsInRelationshipWith.as[Boolean]
//    //insert creates item first and then the listvalues
//
//    val allCreateActions = Vector(createDataItem(createPath, createArgs), createRelayRow(createPath), createCheck, scalarListCreate) ++ createNested
//    val qCreate          = DBIOAction.seq(allCreateActions: _*)
//    //update updates list values first and then the item
//    val allUpdateActions = scalarListUpdate +: updateNested :+ updateDataItemByPath(updatePath, updateArgs)
//    val qUpdate          = DBIOAction.seq(allUpdateActions: _*)
//
//    ifThenElseNestedUpsert(condition, qUpdate, qCreate)

    // fixme: upsert must be completely reimplemented
    ???
  }

  //endregion

  //region DELETE

  def deleteDataItems(model: Model, whereFilter: Option[Filter]) = {
    SimpleDBIO { ctx =>
      val aliasedTable = table(name(schemaName, model.dbName)).as(topLevelAlias)
      val condition    = JooqWhereClauseBuilder(schemaName).buildWhereClause(whereFilter).getOrElse(trueCondition())

      lazy val queryString: String = sql
        .deleteFrom(aliasedTable)
        .where(condition)
        .getSQL

      val ps = ctx.connection.prepareStatement(queryString)
      JooqSetParams.setFilter(new PositionedParameters(ps), whereFilter)
      ps.executeUpdate()
    }
  }

  def deleteRelayIds(model: Model, whereFilter: Option[Filter]): DBIO[_] = {
    SimpleDBIO { ctx =>
      val relayTable      = table(name(schemaName, relayTableName))
      val aliasedTable    = table(name(schemaName, model.dbName)).as(topLevelAlias)
      val filterCondition = JooqWhereClauseBuilder(schemaName).buildWhereClause(whereFilter).getOrElse(trueCondition())
      val condition = field(name(schemaName, relayTableName, "id"))
        .in(select(field(name(topLevelAlias, model.dbNameOfIdField_!))).from(aliasedTable).where(filterCondition))

      lazy val queryString: String = sql
        .deleteFrom(relayTable)
        .where(condition)
        .getSQL

      val ps = ctx.connection.prepareStatement(queryString)
      JooqSetParams.setFilter(new PositionedParameters(ps), whereFilter)
      ps.executeUpdate()
    }
  }

  def deleteDataItemByWhere(where: NodeSelector) = {
    SimpleDBIO[Boolean] { x =>
      lazy val queryString: String = {

        sql
          .deleteFrom(table(name(schemaName, where.model.dbName)))
          .where(field(name(schemaName, where.model.dbName, where.field.dbName)).equal(placeHolder))
          .getSQL
      }

      val statement: PreparedStatement = x.connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS)
      statement.setGcValue(1, where.fieldGCValue)

      statement.execute()
    }
  }

  def deleteDataItemByParentId(parent: RelationField, parentId: IdGCValue) = {
    SimpleDBIO[Boolean] { x =>
      val subSelect = select(field(name(schemaName, parent.relation.relationTableName, parent.oppositeRelationSide.toString)))
        .from(table(name(schemaName, parent.relation.relationTableName)))
        .where(field(name(parent.relation.relationTableName, parent.relationSide.toString)).equal(placeHolder))
      val condition = field(name(schemaName, parent.relatedModel_!.dbName, "id")).equal(subSelect)

      val queryString = sql
        .deleteFrom(table(name(schemaName, parent.relatedModel_!.dbName)))
        .where(condition)
        .getSQL

      val statement: PreparedStatement = x.connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS)
      statement.setGcValue(1, parentId)

      statement.execute()
    }
  }

  def deleteDataItem(path: Path) =
    (sql"""DELETE FROM "#$schemaName"."#${path.lastModel.dbName}" WHERE "#${path.lastModel.dbNameOfIdField_!}" = """ ++ pathQueryForLastChild(path)).asUpdate

  def deleteRelayRowByWhere(where: NodeSelector) = {
    SimpleDBIO[Boolean] { x =>
      lazy val queryString: String = {
        val subSelect = select(field(name(schemaName, where.model.dbName, where.model.dbNameOfIdField_!)))
          .from(table(name(schemaName, where.model.dbName)))
          .where(field(name(schemaName, where.model.dbName, where.field.dbName)).equal(placeHolder))

        sql
          .deleteFrom(table(name(schemaName, relayTableName)))
          .where(field(name(schemaName, relayTableName, "id")).equal(subSelect))
          .getSQL
      }

      val statement: PreparedStatement = x.connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS)
      statement.setGcValue(1, where.fieldGCValue)

      statement.execute()
    }
  }

  def deleteRelayRowByParentId(parent: RelationField, parentId: IdGCValue) = {
    SimpleDBIO[Boolean] { x =>
      val subSelect = select(field(name(schemaName, parent.relation.relationTableName, parent.oppositeRelationSide.toString)))
        .from(table(name(schemaName, parent.relation.relationTableName)))
        .where(field(name(parent.relation.relationTableName, parent.relationSide.toString)).equal(placeHolder))
      val condition = field(name(schemaName, relayTableName, "id")).equal(subSelect)

      val queryString = sql
        .deleteFrom(table(name(schemaName, relayTableName)))
        .where(condition)
        .getSQL

      val statement: PreparedStatement = x.connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS)
      statement.setGcValue(1, parentId)

      statement.execute()
    }
  }

  def deleteRelayRow(path: Path) =
    (sql"""DELETE FROM "#$schemaName"."_RelayId" WHERE "id" = """ ++ pathQueryForLastChild(path)).asUpdate

  def deleteRelationRowByParent(path: Path): DBIO[Unit] = {
    val relation      = path.lastRelation_!
    val relationTable = path.lastRelation_!.relationTableName
    val action = relation.inlineManifestation match {
      case Some(manifestation) =>
        (sql"""UPDATE "#$schemaName"."#$relationTable" """ ++
          sql"""SET "#${manifestation.referencingColumn}" = NULL""" ++
          sql"""WHERE "#${path.columnForParentSideOfLastEdge}" IN """ ++ pathQueryThatUsesWholePath(path.removeLastEdge)).asUpdate
      case None =>
        (sql"""DELETE FROM "#$schemaName"."#$relationTable" WHERE "#${path.columnForParentSideOfLastEdge}" = """ ++ pathQueryForLastParent(path)).asUpdate
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
          sql"""WHERE "#${path.columnForChildSideOfLastEdge}" IN """ ++ pathQueryThatUsesWholePath(path) ++
          sql""" AND "#${path.columnForParentSideOfLastEdge}" IN """ ++ pathQueryThatUsesWholePath(path.removeLastEdge)).asUpdate
      case None =>
        (sql"""DELETE FROM "#$schemaName"."#$relationTable" WHERE "#${path.columnForChildSideOfLastEdge}"""" ++ idFromWhereEquals(where)).asUpdate
    }
    action.andThen(dbioUnit)
  }

  def deleteRelationRowByParentAndChild(path: Path) = {
    val relation = path.lastRelation_!
    relation.inlineManifestation match {
      case Some(manifestation) =>
        (sql"""UPDATE "#$schemaName"."#${path.lastRelation_!.relationTableName}"  """ ++
          sql"""SET "#${manifestation.referencingColumn}" = NULL """ ++
          sql"""WHERE "#${path.columnForChildSideOfLastEdge}" = """ ++ pathQueryForLastChild(path) ++
          sql""" AND "#${path.columnForParentSideOfLastEdge}" = """ ++ pathQueryForLastParent(path)).asUpdate.andThen(dbioUnit)
      case _ =>
        (sql"""DELETE FROM "#$schemaName"."#${path.lastRelation_!.relationTableName}" """ ++
          sql"""WHERE "#${path.columnForChildSideOfLastEdge}" = """ ++ pathQueryForLastChild(path) ++
          sql""" AND "#${path.columnForParentSideOfLastEdge}" = """ ++ pathQueryForLastParent(path)).asUpdate.andThen(dbioUnit)
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
  def setScalarList(where: NodeSelector, listFieldMap: Vector[(String, ListGCValue)]) = {
    val idQuery = SimpleDBIO { ctx =>
      lazy val queryString: String = sql.select(value(placeHolder)).getSQL
      val ps                       = ctx.connection.prepareStatement(queryString)
      val pp                       = new PositionedParameters(ps)
      pp.setGcValue(where.fieldGCValue)
      val rs = ps.executeQuery()
      rs.as(readFirstColumnAsString)
    }

    if (listFieldMap.isEmpty) DBIOAction.successful(()) else setManyScalarListHelper(where.model, listFieldMap, idQuery)
  }

  def setManyScalarLists(model: Model, listFieldMap: Vector[(String, ListGCValue)], whereFilter: Option[Filter]) = {
    val idQuery = SimpleDBIO { ctx =>
      val condition    = JooqWhereClauseBuilder(schemaName).buildWhereClause(whereFilter).getOrElse(trueCondition())
      val aliasedTable = table(name(schemaName, model.dbName)).as(topLevelAlias)

      val queryString = select(field(name(topLevelAlias, model.dbNameOfIdField_!)))
        .from(aliasedTable)
        .where(condition)
        .getSQL

      val ps = ctx.connection.prepareStatement(queryString)
      JooqSetParams.setFilter(new PositionedParameters(ps), whereFilter)
      val rs = ps.executeQuery()
      rs.as(readFirstColumnAsString)
    }

    if (listFieldMap.isEmpty) DBIOAction.successful(()) else setManyScalarListHelper(model, listFieldMap, idQuery)
  }

  def setManyScalarListHelper(model: Model, listFieldMap: Vector[(String, ListGCValue)], idQuery: DBIO[Vector[String]]) = {
    import scala.concurrent.ExecutionContext.Implicits.global

    def listInsert(ids: Vector[String]) = {
      if (ids.isEmpty) {
        DBIOAction.successful(())
      } else {

        SimpleDBIO[Unit] { x =>
          def valueTuplesForListField(listGCValue: ListGCValue) = {
            for {
              nodeId                   <- ids
              (escapedValue, position) <- listGCValue.values.zip((1 to listGCValue.size).map(_ * 1000))
            } yield {
              (nodeId, position, escapedValue)
            }
          }

          listFieldMap.foreach {
            case (fieldName, listGCValue) =>
              val dbNameOfField = model.getFieldByName_!(fieldName).dbName
              val tableName     = s"${model.dbName}_$dbNameOfField"

              val condition = ids.length match {
                case 1 => field(name(schemaName, tableName, nodeIdFieldName)).equal(placeHolder)
                case _ => field(name(schemaName, tableName, nodeIdFieldName)).in(ids.map(_ => placeHolder): _*)
              }

              val wipe = sql
                .deleteFrom(table(name(schemaName, tableName)))
                .where(condition)
                .getSQL

              val wipeOldValues: PreparedStatement = x.connection.prepareStatement(wipe)
              ids.zipWithIndex.foreach { zip =>
                wipeOldValues.setString(zip._2 + 1, zip._1)
              }

              wipeOldValues.executeUpdate()

              val insert = sql
                .insertInto(table(name(schemaName, tableName)))
                .columns(
                  field(name(schemaName, tableName, nodeIdFieldName)),
                  field(name(schemaName, tableName, positionFieldName)),
                  field(name(schemaName, tableName, valueFieldName))
                )
                .values(placeHolder, placeHolder, placeHolder)
                .getSQL

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

  def idFromWhere(where: NodeSelector): SQLActionBuilder = (where.isId, where.fieldGCValue) match {
    case (true, NullGCValue) => sys.error("id should not be NULL")
    case (true, idValue)     => sql"$idValue"
    case (false, NullGCValue) =>
      sql"""(SELECT "#${where.model.dbNameOfIdField_!}" FROM "#$schemaName"."#${where.model.dbName}" IDFROMWHERE WHERE "#${where.field.dbName}" is NULL)"""
    case (false, value) =>
      sql"""(SELECT "#${where.model.dbNameOfIdField_!}" FROM "#$schemaName"."#${where.model.dbName}" IDFROMWHERE WHERE "#${where.field.dbName}" = $value)"""
  }

  def idFromWhereEquals(where: NodeSelector): SQLActionBuilder = sql" = " ++ idFromWhere(where)

  def idFromWherePath(where: NodeSelector): SQLActionBuilder = {
    sql"""(SELECT "#${where.model.dbNameOfIdField_!}" FROM "#$schemaName"."#${where.model.dbName}" IDFROMWHEREPATH WHERE "#${where.field.dbName}" = ${where.fieldGCValue})"""
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
          sql""" FROM "#$schemaName"."#${last.relation.relationTableName}" PATHQUERY""" ++
          sql" WHERE " ++ childWhere ++ sql""""#${last.columnForParentRelationSide}" IN (""" ++ pathQueryForLastParent(path) ++ sql"))"
    }
  }

  def whereFailureTrigger(where: NodeSelector, causeString: String) = {
    val table = where.model.dbName
    val query =
      sql"""(SELECT "#${where.model.dbNameOfIdField_!}" FROM "#$schemaName"."#${table}" WHEREFAILURETRIGGER WHERE "#${where.field.dbName}" = ${where.fieldGCValue})"""

    triggerFailureWhenNotExists(query, causeString)
  }

  def connectionFailureTrigger(path: Path, causeString: String) = {
    val table = path.lastRelation.get.relationTableName

    val lastChildWhere = path.lastEdge_! match {
      case edge: NodeEdge => sql""" "#${path.columnForChildSideOfLastEdge}"""" ++ idFromWhereEquals(edge.childWhere) ++ sql" AND "
      case _: ModelEdge   => sql""" "#${path.columnForChildSideOfLastEdge}" IS NOT NULL AND """
    }

    val query =
      sql"""SELECT * FROM "#$schemaName"."#$table" CONNECTIONFAILURETRIGGERPATH""" ++
        sql"WHERE" ++ lastChildWhere ++ sql""""#${path.columnForParentSideOfLastEdge}" = """ ++ pathQueryForLastParent(path)

    triggerFailureWhenNotExists(query, causeString)
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

    triggerFailureWhenExists(query, triggerString)
  }

  def oldParentFailureTrigger(path: Path, triggerString: String) = {
    val table = path.lastRelation_!.relationTableName
    val query = sql"""SELECT * FROM "#$schemaName"."#$table" OLDPARENTPATHFAILURETRIGGER WHERE "#${path.columnForChildSideOfLastEdge}" IN (""" ++
      pathQueryForLastChild(path) ++ sql")"
    triggerFailureWhenExists(query, triggerString)
  }

  def oldParentFailureTriggerByField(path: Path, field: RelationField, triggerString: String) = {
    val relation       = field.relation
    val table          = relation.relationTableName
    val oppositeColumn = relation.columnForRelationSide(field.oppositeRelationSide)
    val column         = relation.columnForRelationSide(field.relationSide)
    val query = sql"""SELECT * FROM "#$schemaName"."#$table" OLDPARENTPATHFAILURETRIGGERBYFIELD""" ++
      sql"""WHERE "#$oppositeColumn" IN (""" ++ pathQueryForLastChild(path) ++ sql") " ++
      sql"""AND "#$column" IS NOT NULL"""
    triggerFailureWhenExists(query, triggerString)
  }

  def oldParentFailureTriggerByFieldAndFilter(model: Model, whereFilter: Option[Filter], field: RelationField, causeString: String): DBIO[_] = {
    val relation = field.relation
    val table    = relation.relationTableName

    val column         = relation.columnForRelationSide(field.oppositeRelationSide)
    val oppositeColumn = relation.columnForRelationSide(field.relationSide)

    SimpleDBIO { ctx =>
      val innerQuery =
        s"""SELECT * FROM "$schemaName"."$table" OLDPARENTPATHFAILURETRIGGERBYFIELDANDFILTER """ +
          s"""WHERE "$oppositeColumn" IS NOT NULL """ +
          s"""AND "$column" IN (""" +
          s"""SELECT "${model.dbNameOfIdField_!}" FROM "$schemaName"."${model.dbName}" as "$topLevelAlias" """ +
          WhereClauseBuilder(schemaName).buildWhereClause(whereFilter).getOrElse("") + ")"

      val query = triggerFailureWhenExists(innerQuery, causeString)
      val ps    = ctx.connection.prepareStatement(query)

      SetParams.setFilter(ps, whereFilter)
      ps.executeQuery()
    }
  }

  def oldChildFailureTrigger(path: Path, triggerString: String) = {
    val table = path.lastRelation_!.relationTableName
    val query = sql"""SELECT * FROM "#$schemaName"."#$table" OLDCHILDPATHFAILURETRIGGER""" ++
      sql"""WHERE "#${path.columnForParentSideOfLastEdge}" IN (""" ++ pathQueryForLastParent(path) ++ sql") " ++
      sql"""AND "#${path.columnForChildSideOfLastEdge}" IS NOT NULL """
    triggerFailureWhenExists(query, triggerString)
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

  def triggerFailureWhenExists(query: String, triggerString: String) = triggerFailureInternal(query, triggerString, notExists = false)

  def triggerFailureWhenExists(query: SQLActionBuilder, triggerString: String)    = triggerFailureInternal(query, triggerString, notExists = false)
  def triggerFailureWhenNotExists(query: SQLActionBuilder, triggerString: String) = triggerFailureInternal(query, triggerString, notExists = true)

  private def triggerFailureInternal(query: SQLActionBuilder, triggerString: String, notExists: Boolean) = {
    val notValue = if (notExists) s"" else s"not"

    (sql"select case when #$notValue exists ( " ++ query ++ sql" ) " ++
      sql"then '' " ++
      sql"""else ("#$schemaName".raise_exception($triggerString))end;""").as[String]
  }

  private def triggerFailureInternal(query: String, triggerString: String, notExists: Boolean) = {
    val notValue = if (notExists) s"" else s"not"

    s"select case when $notValue exists ( " + query + " )" +
      "then '' " +
      s"""else ("$schemaName".raise_exception('$triggerString'))end;"""
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
        case e: Exception => Vector(e.getMessage)
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
        case e: Exception => Vector(e.getMessage)
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
              s"Failure inserting into listTable $tableName for the id $nodeId for value ${failedValue.value}. Cause: ${removeConnectionInfoFromCause(e.getCause.toString)}"
            }
            .toVector

        case e: Exception => Vector(e.getMessage)
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

  private val readFirstColumnAsString: ReadsResultSet[String] = ReadsResultSet(_.getString(1))
}
