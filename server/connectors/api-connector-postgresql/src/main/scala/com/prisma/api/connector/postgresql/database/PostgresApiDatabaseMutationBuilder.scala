package com.prisma.api.connector.postgresql.database

import java.sql.{PreparedStatement, ResultSet, Statement}
import java.util.Date

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.JdbcExtensions._
import com.prisma.api.connector.postgresql.database.JooqExtensions._
import com.prisma.api.connector.postgresql.database.PostgresSlickExtensions._
import com.prisma.api.schema.APIErrors.{NodesNotConnectedError, RequiredRelationWouldBeViolated}
import com.prisma.gc_values.{ListGCValue, NullGCValue, _}
import com.prisma.shared.models.Manifestations.InlineRelationManifestation
import com.prisma.shared.models.TypeIdentifier.IdTypeIdentifier
import com.prisma.shared.models._
import com.prisma.slick.NewJdbcExtensions._
import cool.graph.cuid.Cuid
import org.joda.time.{DateTime, DateTimeZone}
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DSL._
import org.jooq.{Field, Query => JooqQuery, _}
import slick.dbio.DBIOAction
import slick.jdbc.{MySQLProfile, PositionedParameters, PostgresProfile}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

trait BuilderBase {
  import JooqQueryBuilders.placeHolder

  def schemaName: String

  val slickDatabase: SlickDatabase
  val dialect: SQLDialect = slickDatabase.profile match {
    case PostgresProfile => SQLDialect.POSTGRES_9_5
    case MySQLProfile    => SQLDialect.MYSQL_5_7
    case x               => sys.error(s"No Jooq SQLDialect for Slick profile $x configured yet")
  }

  import slickDatabase.profile.api._

  val sql = DSL.using(dialect, new Settings().withRenderFormatted(true))

  private val relayIdTableName = "_RelayId"

  val relayIdColumn                                                                         = field(name(schemaName, relayIdTableName, "id"))
  val relayStableIdentifierColumn                                                           = field(name(schemaName, relayIdTableName, "stableModelIdentifier"))
  val relayTable                                                                            = table(name(schemaName, relayIdTableName))
  def idField(model: Model)                                                                 = field(name(schemaName, model.dbName, model.dbNameOfIdField_!))
  def modelTable(model: Model)                                                              = table(name(schemaName, model.dbName))
  def relationTable(relation: Relation)                                                     = table(name(schemaName, relation.relationTableName))
  def scalarListTable(field: ScalarField)                                                   = table(name(schemaName, scalarListTableName(field)))
  def modelColumn(model: Model, scalarField: com.prisma.shared.models.Field): Field[AnyRef] = field(name(schemaName, model.dbName, scalarField.dbName))
  def modelIdColumn(model: Model)                                                           = field(name(schemaName, model.dbName, model.dbNameOfIdField_!))
  def relationColumn(relation: Relation, side: RelationSide.Value)                          = field(name(schemaName, relation.relationTableName, relation.columnForRelationSide(side)))
  def relationIdColumn(relation: Relation)                                                  = field(name(schemaName, relation.relationTableName, "id"))
  def inlineRelationColumn(relation: Relation, mani: InlineRelationManifestation)           = field(name(schemaName, relation.relationTableName, mani.referencingColumn))
  def scalarListColumn(scalarField: ScalarField, column: String)                            = field(name(schemaName, scalarListTableName(scalarField), column))
  def column(table: String, column: String)                                                 = field(name(schemaName, table, column))
  def aliasColumn(column: String)                                                           = field(name(JooqQueryBuilders.topLevelAlias, column))
  def placeHolders(vector: Iterable[Any])                                                   = vector.toList.map(_ => placeHolder).asJava
  private def scalarListTableName(field: ScalarField)                                       = field.model.dbName + "_" + field.dbName

  val isMySql = dialect.family() == SQLDialect.MYSQL

  def queryToDBIO[T](query: JooqQuery)(setParams: PositionedParameters => Unit, readResult: ResultSet => T): DBIO[T] = {
    SimpleDBIO { ctx =>
      val ps = ctx.connection.prepareStatement(query.getSQL)
      val pp = new PositionedParameters(ps)
      setParams(pp)

      val rs = ps.executeQuery()
      readResult(rs)
    }
  }

  def deleteToDBIO(query: Delete[Record])(setParams: PositionedParameters => Unit): DBIO[Unit] = {
    SimpleDBIO { ctx =>
      val ps = ctx.connection.prepareStatement(query.getSQL)
      val pp = new PositionedParameters(ps)
      setParams(pp)

      ps.execute()
    }
  }

  def updateToDBIO(query: Update[Record])(setParams: PositionedParameters => Unit): DBIO[Unit] = {
    SimpleDBIO { ctx =>
      val ps = ctx.connection.prepareStatement(query.getSQL)
      val pp = new PositionedParameters(ps)
      setParams(pp)

      ps.executeUpdate()
    }
  }

  def truncateToDBIO(query: Truncate[Record]): DBIO[Unit] = {
    SimpleDBIO { ctx =>
      val ps = ctx.connection.prepareStatement(query.getSQL)
      ps.executeUpdate()
    }
  }

  def readPrismaNodeWithParent(rf: RelationField) = ReadsResultSet { rs =>
    val node = readPrismaNode(rf.relatedModel_!, rs)

    val parentId = if (rf.relation.isSameModelRelation) {
      val firstSide  = rs.getParentId(RelationSide.A, rf.model.idField_!.typeIdentifier)
      val secondSide = rs.getParentId(RelationSide.B, rf.model.idField_!.typeIdentifier)
      if (firstSide == node.id) secondSide else firstSide
    } else {
      val parentRelationSide = rf.relation.modelA match {
        case x if x == rf.relatedModel_! => RelationSide.B
        case _                           => RelationSide.A
      }
      rs.getParentId(parentRelationSide, rf.model.idField_!.typeIdentifier)
    }
    PrismaNodeWithParent(parentId, node)
  }

  def readsPrismaNode(model: Model): ReadsResultSet[PrismaNode] = ReadsResultSet { rs =>
    readPrismaNode(model, rs)
  }

  private def readPrismaNode(model: Model, rs: ResultSet) = {
    val data = model.scalarNonListFields.map(field => field.name -> rs.getGcValue(field.dbName, field.typeIdentifier))
    PrismaNode(id = rs.getId(model), data = RootGCValue(data: _*), Some(model.name))
  }
}

case class PostgresApiDatabaseMutationBuilder(
    schemaName: String,
    slickDatabase: SlickDatabase
) extends BuilderBase
    with ImportActions {
  import JooqQueryBuilders._
  import slickDatabase.profile.api._

  // region CREATE

  def createDataItem(model: Model, args: PrismaArgs): DBIO[IdGCValue] = {
    SimpleDBIO { x =>
      val idIsAutoGenerated = model.idField_!.isAutoGenerated
      val argsAsRoot = if (idIsAutoGenerated) {
        args.raw.asRoot
      } else {
        args.raw.asRoot.add(model.idField_!.name, generateId(model))
      }
      val fields = model.fields.filter(field => argsAsRoot.hasArgFor(field.name))

      val query = sql
        .insertInto(modelTable(model))
        .columns(fields.map(field => modelColumn(model, field)): _*)
        .values(placeHolders(fields))

      val itemInsert: PreparedStatement = x.connection.prepareStatement(query.getSQL, Statement.RETURN_GENERATED_KEYS)

      val currentTimestamp = currentTimeStampUTC
      fields.map(_.name).zipWithIndex.foreach {
        case (column, index) =>
          argsAsRoot.map.get(column) match {
            case Some(NullGCValue) if column == createdAtField || column == updatedAtField => itemInsert.setTimestamp(index + 1, currentTimestamp)
            case Some(gCValue)                                                             => itemInsert.setGcValue(index + 1, gCValue)
            case None if column == createdAtField || column == updatedAtField              => itemInsert.setTimestamp(index + 1, currentTimestamp)
            case None                                                                      => itemInsert.setNull(index + 1, java.sql.Types.NULL)
          }
      }
      itemInsert.execute()

      if (idIsAutoGenerated) {
        val generatedKeys = itemInsert.getGeneratedKeys
        generatedKeys.next()
        generatedKeys.getId(model)
      } else {
        argsAsRoot.idField
      }
    }
  }

  def generateId(model: Model) = {
    model.idField_!.typeIdentifier.asInstanceOf[IdTypeIdentifier] match {
      case TypeIdentifier.UUID => UuidGCValue.random()
      case TypeIdentifier.Cuid => CuidGCValue.random()
      case TypeIdentifier.Int  => sys.error("can't generate int ids")
    }
  }

  def createRelayRowById(model: Model, id: IdGCValue): DBIO[_] = {
    SimpleDBIO[Boolean] { x =>
      lazy val queryString: String = {
        sql
          .insertInto(relayTable)
          .columns(relayIdColumn, relayStableIdentifierColumn)
          .values(placeHolder, placeHolder)
          .getSQL
      }

      val statement: PreparedStatement = x.connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS)
      statement.setGcValue(1, id)
      statement.setString(2, model.stableIdentifier)

      statement.execute()
    }
  }

  def createRelation(relationField: RelationField, parentId: IdGCValue, childId: IdGCValue): DBIO[_] = {
    val relation = relationField.relation

    if (relation.isInlineRelation) {
      val inlineManifestation  = relation.inlineManifestation.get
      val referencingColumn    = inlineManifestation.referencingColumn
      val childModel           = relationField.relatedModel_!
      val parentModel          = relationField.model
      val childWhereCondition  = idField(childModel).equal(placeHolder)
      val parentWhereCondition = idField(parentModel).equal(placeHolder)

      val (idToLinkTo, idToUpdate, rowToUpdateCondition) = if (relation.isSameModelRelation) {
        if (relationField.relationSide == RelationSide.B) {
          (childId, parentId, childWhereCondition)
        } else {
          (parentId, childId, parentWhereCondition)
        }
      } else {
        if (inlineManifestation.inTableOfModelId == childModel.name) {
          (parentId, childId, childWhereCondition)
        } else {
          (childId, parentId, parentWhereCondition)
        }
      }

      val query = sql
        .update(relationTable(relation))
        .setColumnsWithPlaceHolders(Vector(referencingColumn))
        .where(rowToUpdateCondition)

      updateToDBIO(query)(
        setParams = { pp =>
          pp.setGcValue(idToLinkTo)
          pp.setGcValue(idToUpdate)
        }
      )
    } else if (relation.hasManifestation) {
      SimpleDBIO[Boolean] { x =>
        lazy val queryString: String = {
          sql
            .insertInto(relationTable(relation))
            .columns(
              relationColumn(relation, relationField.relationSide),
              relationColumn(relation, relationField.oppositeRelationSide)
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
          sql
            .insertInto(relationTable(relation))
            .columns(
              relationIdColumn(relation),
              relationColumn(relation, relationField.relationSide),
              relationColumn(relation, relationField.oppositeRelationSide)
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
        val aliasedTable = modelTable(model).as(topLevelAlias)
        val condition    = JooqWhereClauseBuilder(slickDatabase, schemaName).buildWhereClause(whereFilter).getOrElse(trueCondition())

        val base = sql.update(aliasedTable)

        //https://www.postgresql.org/message-id/20170719174507.GA19616%40telsasoft.com
        lazy val queryString: String = if (map.size > 1) {
          val columns = map.map { case (k, _) => model.getFieldByName_!(k).dbName }.toList

          base
            .setColumnsWithPlaceHolders(columns)
            .where(condition)
            .getSQL

        } else {
          val fieldDef = map.map { case (k, _) => field(model.getFieldByName_!(k).dbName) }.head

          base
            .set(fieldDef, placeHolder)
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

  def updateDataItemById(model: Model, id: IdGCValue, updateArgs: PrismaArgs): DBIO[_] = {
    if (updateArgs.raw.asRoot.map.isEmpty) {
      DBIOAction.successful(id)
    } else {
      SimpleDBIO { ctx =>
        val actualArgs = addUpdatedAt(model, updateArgs.raw.asRoot)
        val columns    = actualArgs.map.map { case (k, _) => model.getFieldByName_!(k).dbName }.toList
        val values     = actualArgs.map.map { case (_, v) => v }

        val query = sql
          .update(modelTable(model))
          .setColumnsWithPlaceHolders(columns)
          .where(idField(model).equal(placeHolder))

        val ps = ctx.connection.prepareStatement(query.getSQL)
        val pp = new PositionedParameters(ps)

        values.foreach(pp.setGcValue)
        pp.setGcValue(id)

        ps.execute()

        id
      }
    }
  }

  //endregion

  private def addUpdatedAt(model: Model, updateValues: RootGCValue): RootGCValue = {
    model.updatedAtField match {
      case Some(updatedAtField) =>
        val today              = new Date()
        val exactlyNow         = new DateTime(today).withZone(DateTimeZone.UTC)
        val currentDateGCValue = DateTimeGCValue(exactlyNow)
        updateValues.add(updatedAtField.name, currentDateGCValue)
      case None =>
        updateValues
    }
  }

  //region DELETE

  def deleteNodeById(model: Model, id: IdGCValue)(implicit ec: ExecutionContext) = deleteNodes(model, Vector(id))

  //Todo check how much of a performance gain it would be to chain these using andThen instead of the for comprehension
  def deleteNodes(model: Model, ids: Vector[IdGCValue])(implicit ec: ExecutionContext): DBIO[Unit] = {
    for {
      _ <- deleteScalarListEntriesByIds(model, ids)
      _ <- deleteRelayRowsByIds(ids)
      _ <- deleteDataItemsByIds(model, ids)
    } yield ()
  }

  private def deleteDataItemsByIds(model: Model, ids: Vector[IdGCValue]): DBIO[Unit] = {
    val query = sql
      .deleteFrom(modelTable(model))
      .where(idField(model).in(placeHolders(ids)))

    deleteToDBIO(query)(
      setParams = pp => ids.foreach(pp.setGcValue)
    )
  }

  private def deleteRelayRowsByIds(ids: Vector[IdGCValue]): DBIO[Unit] = {
    val query = sql
      .deleteFrom(relayTable)
      .where(relayIdColumn.in(placeHolders(ids)))

    deleteToDBIO(query)(
      setParams = pp => ids.foreach(pp.setGcValue)
    )
  }

  private def deleteScalarListEntriesByIds(model: Model, ids: Vector[IdGCValue]): DBIO[Unit] = {

    val actions = model.scalarListFields.map { listField =>
      val query = sql
        .deleteFrom(scalarListTable(listField))
        .where(scalarListColumn(listField, "nodeId").in(placeHolders(ids)))

      deleteToDBIO(query)(
        setParams = pp => ids.foreach(pp.setGcValue)
      )
    }
    DBIO.seq(actions: _*)
  }

  def parentIdCondition(parentField: RelationField): Condition = parentIdCondition(parentField, Vector(1))

  def parentIdCondition(parentField: RelationField, parentIds: Vector[Any]): Condition = {
    val relation      = parentField.relation
    val childIdField  = relationColumn(relation, parentField.oppositeRelationSide)
    val parentIdField = relationColumn(relation, parentField.relationSide)
    val subSelect = sql
      .select(childIdField)
      .from(relationTable(relation))
      .where(parentIdField.in(placeHolders(parentIds)))

    idField(parentField.relatedModel_!).in(subSelect)
  }

  def deleteRelationRowByChildId(relationField: RelationField, childId: IdGCValue): DBIO[Unit] = {
    val relation  = relationField.relation
    val condition = relationColumn(relation, relationField.oppositeRelationSide).equal(placeHolder)

    relation.inlineManifestation match {
      case Some(manifestation) =>
        val query = sql
          .update(relationTable(relation))
          .set(inlineRelationColumn(relation, manifestation), placeHolder)
          .where(condition)

        updateToDBIO(query)(
          setParams = { pp =>
            pp.setGcValue(NullGCValue)
            pp.setGcValue(childId)
          }
        )

      case None =>
        val query = sql
          .deleteFrom(relationTable(relation))
          .where(condition)
        deleteToDBIO(query)(setParams = _.setGcValue(childId))
    }

  }

  def deleteRelationRowByParentId(relationField: RelationField, parentId: IdGCValue): DBIO[Unit] = {
    val relation  = relationField.relation
    val condition = relationColumn(relation, relationField.relationSide).equal(placeHolder)
    relation.inlineManifestation match {
      case Some(manifestation) =>
        val query = sql
          .update(relationTable(relation))
          .set(inlineRelationColumn(relation, manifestation), placeHolder)
          .where(condition)

        updateToDBIO(query)(setParams = { pp =>
          pp.setGcValue(NullGCValue)
          pp.setGcValue(parentId)
        })

      case None =>
        val query = sql
          .deleteFrom(relationTable(relation))
          .where(condition)

        deleteToDBIO(query)(setParams = _.setGcValue(parentId))
    }

  }

  //endregion

  //region SCALAR LISTS
  def setScalarListById(model: Model, id: IdGCValue, listFieldMap: Vector[(String, ListGCValue)]) = {
    if (listFieldMap.isEmpty) DBIOAction.successful(()) else setManyScalarListHelper(model, listFieldMap, DBIO.successful(Vector(id)))
  }

  def setManyScalarLists(model: Model, listFieldMap: Vector[(String, ListGCValue)], whereFilter: Option[Filter]) = {
    val idQuery = SimpleDBIO { ctx =>
      val condition    = JooqWhereClauseBuilder(slickDatabase, schemaName).buildWhereClause(whereFilter).getOrElse(trueCondition())
      val aliasedTable = modelTable(model).as(topLevelAlias)

      val queryString = sql
        .select(aliasColumn(model.dbNameOfIdField_!))
        .from(aliasedTable)
        .where(condition)
        .getSQL

      val ps = ctx.connection.prepareStatement(queryString)
      JooqSetParams.setFilter(new PositionedParameters(ps), whereFilter)
      val rs = ps.executeQuery()
      rs.as(readId(model))
    }

    if (listFieldMap.isEmpty) DBIOAction.successful(()) else setManyScalarListHelper(model, listFieldMap, idQuery)
  }

  def setManyScalarListHelper(model: Model, listFieldMap: Vector[(String, ListGCValue)], idQuery: DBIO[Vector[IdGCValue]]) = {
    import scala.concurrent.ExecutionContext.Implicits.global

    def listInsert(ids: Vector[IdGCValue]) = {
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
                wipeOldValues.setGcValue(zip._2 + 1, zip._1)
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
                insertNewValues.setGcValue(1, tuple._1)
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
  def truncateTables(project: Project): DBIO[_] = {
    val relationTables = project.relations.map(relationTable)
    val modelTables    = project.models.map(modelTable)
    val listTables     = project.models.flatMap(model => model.scalarListFields.map(scalarListTable))
    val actions = (relationTables ++ listTables ++ Vector(relayTable) ++ modelTables).map { table =>
      if (isMySql) {
        truncateToDBIO(sql.truncate(table))
      } else {
        truncateToDBIO(sql.truncate(table).cascade())
      }
    }
    val truncatesAction = DBIO.sequence(actions)

    def disableForeignKeyChecks = SimpleDBIO { ctx =>
      val ps = ctx.connection.prepareStatement("SET FOREIGN_KEY_CHECKS=0")
      ps.executeUpdate()
    }
    def enableForeignKeyChecks = SimpleDBIO { ctx =>
      val ps = ctx.connection.prepareStatement("SET FOREIGN_KEY_CHECKS=1")
      ps.executeUpdate()
    }

    if (isMySql) {
      DBIO.seq(disableForeignKeyChecks, truncatesAction, enableForeignKeyChecks)
    } else {
      truncatesAction
    }
  }

  //endregion

  // region HELPERS

  def queryNodeByWhere(where: NodeSelector): DBIO[Option[PrismaNode]] = {
    val model = where.model
    val query = sql
      .select(asterisk())
      .from(modelTable(model))
      .where(modelColumn(model, where.field).equal(placeHolder))

    queryToDBIO(query)(
      setParams = pp => pp.setGcValue(where.fieldGCValue),
      readResult = rs => rs.as(readsPrismaNode(model)).headOption
    )
  }

  def queryIdFromWhere(where: NodeSelector): DBIO[Option[IdGCValue]] = {
    SimpleDBIO { ctx =>
      val model = where.model
      val query = sql
        .select(idField(model))
        .from(modelTable(model))
        .where(modelColumn(model, where.field).equal(placeHolder))

      val ps = ctx.connection.prepareStatement(query.getSQL)
      ps.setGcValue(1, where.fieldGCValue)

      val rs = ps.executeQuery()

      if (rs.next()) {
        Some(rs.getId(model))
      } else {
        None
      }
    }
  }

  def queryIdByParentId(parentField: RelationField, parentId: IdGCValue)(implicit ec: ExecutionContext): DBIO[Option[IdGCValue]] = {
    queryIdsByParentIds(parentField, Vector(parentId)).map(_.headOption)
  }

  def queryIdsByParentIds(parentField: RelationField, parentIds: Vector[IdGCValue]): DBIO[Vector[IdGCValue]] = {
    val model = parentField.relatedModel_!
    val q: SelectConditionStep[Record1[AnyRef]] = sql
      .select(idField(model))
      .from(modelTable(model))
      .where(parentIdCondition(parentField, parentIds))
    queryToDBIO(q)(
      setParams = pp => parentIds.foreach(pp.setGcValue),
      readResult = rs => rs.as(readId(model))
    )
  }

  def queryIdsByWhereFilter(model: Model, filter: Option[Filter]): DBIO[Vector[IdGCValue]] = {
    val aliasedTable    = modelTable(model).as(topLevelAlias)
    val filterCondition = JooqWhereClauseBuilder(slickDatabase, schemaName).buildWhereClause(filter).getOrElse(trueCondition())
    val query           = sql.select(field(name(topLevelAlias, model.dbNameOfIdField_!))).from(aliasedTable).where(filterCondition)

    queryToDBIO(query)(
      setParams = pp => JooqSetParams.setFilter(pp, filter),
      readResult = rs => rs.as(readId(model))
    )
  }

  def queryIdByParentIdAndWhere(parentField: RelationField, parentId: IdGCValue, where: NodeSelector): DBIO[Option[IdGCValue]] = {
    val model                 = parentField.relatedModel_!
    val nodeSelectorCondition = modelColumn(model, where.field).equal(placeHolder)
    val q: SelectConditionStep[Record1[AnyRef]] = sql
      .select(idField(model))
      .from(modelTable(model))
      .where(parentIdCondition(parentField), nodeSelectorCondition)

    queryToDBIO(q)(
      setParams = { pp =>
        pp.setGcValue(parentId)
        pp.setGcValue(where.fieldGCValue)
      },
      readResult = { rs =>
        if (rs.next()) {
          Some(rs.getId(model))
        } else {
          None
        }
      }
    )
  }

  def ensureThatNodeIsNotConnected(
      relationField: RelationField,
      childId: IdGCValue
  )(implicit ec: ExecutionContext): DBIO[Unit] = {
    val relation = relationField.relation
    val idQuery = sql
      .select(asterisk())
      .from(relationTable(relation))
      .where(relationColumn(relation, relationField.oppositeRelationSide).equal(placeHolder))
      .and(relationColumn(relation, relationField.relationSide).isNotNull)

    val action = queryToDBIO(idQuery)(
      setParams = _.setGcValue(childId),
      readResult = rs => rs.as(readsAsUnit)
    )
    action.map { result =>
      if (result.nonEmpty) throw RequiredRelationWouldBeViolated(relation)
    }
  }

  def ensureThatNodeIsConnected(
      relationField: RelationField,
      childId: IdGCValue
  )(implicit ec: ExecutionContext): DBIO[Unit] = {
    val relation = relationField.relation
    val idQuery = sql
      .select(asterisk())
      .from(relationTable(relation))
      .where(relationColumn(relation, relationField.oppositeRelationSide).equal(placeHolder))
      .and(relationColumn(relation, relationField.relationSide).isNotNull)

    val action = queryToDBIO(idQuery)(
      setParams = _.setGcValue(childId),
      readResult = rs => rs.as(readsAsUnit)
    )
    action.map { result =>
      if (result.isEmpty)
        throw NodesNotConnectedError(
          relation = relationField.relation,
          parent = relationField.model,
          parentWhere = None,
          child = relationField.relatedModel_!,
          childWhere = Some(NodeSelector.forIdGCValue(relationField.relatedModel_!, childId))
        )
    }
  }

  def ensureThatParentIsConnected(
      relationField: RelationField,
      parentId: IdGCValue
  )(implicit ec: ExecutionContext): DBIO[Unit] = {
    val relation = relationField.relation
    val idQuery = sql
      .select(asterisk())
      .from(relationTable(relation))
      .where(relationColumn(relation, relationField.relationSide).equal(placeHolder))
      .and(relationColumn(relation, relationField.oppositeRelationSide).isNotNull)

    val action = queryToDBIO(idQuery)(
      setParams = _.setGcValue(parentId),
      readResult = rs => rs.as(readsAsUnit)
    )
    action.map { result =>
      if (result.isEmpty)
        throw NodesNotConnectedError(
          relation = relationField.relation,
          parent = relationField.model,
          parentWhere = Some(NodeSelector.forIdGCValue(relationField.model, parentId)),
          child = relationField.relatedModel_!,
          childWhere = None
        )
    }
  }

  def oldParentFailureTriggerByField(parentId: IdGCValue, field: RelationField)(implicit ec: ExecutionContext): DBIO[Unit] = {
    oldParentFailureTriggerByField(Vector(parentId), field)
  }

  def oldParentFailureTriggerByField(parentIds: Vector[IdGCValue], field: RelationField)(implicit ec: ExecutionContext): DBIO[Unit] = {
    val relation = field.relation
    val query = sql
      .select(asterisk())
      .from(relationTable(relation))
      .where(
        relationColumn(relation, field.oppositeRelationSide).in(placeHolders(parentIds)),
        relationColumn(relation, field.relationSide).isNotNull
      )

    val action = queryToDBIO(query)(
      setParams = pp => parentIds.foreach(pp.setGcValue),
      readResult = rs => rs.as(readsAsUnit)
    )
    action.map { result =>
      if (result.nonEmpty) {
        // fixme: decide which error to use
        throw RequiredRelationWouldBeViolated(relation)
        //        throw RelationIsRequired(field.name, field.model.name)
      }
    }
  }

  def oldParentFailureTriggerByFieldAndFilter(model: Model, whereFilter: Option[Filter], relationField: RelationField)(
      implicit ec: ExecutionContext): DBIO[_] = {
    val relation = relationField.relation
    val query = sql
      .select(asterisk())
      .from(relationTable(relation))
      .where(relationColumn(relation, relationField.relationSide).isNotNull)
      .and(relationColumn(relation, relationField.oppositeRelationSide).in {
        sql
          .select(field(name("Alias", model.dbNameOfIdField_!)))
          .from(modelTable(model).as("Alias"))
          .where(JooqWhereClauseBuilder(slickDatabase, schemaName).buildWhereClause(whereFilter).getOrElse(trueCondition()))
      })

    val action = queryToDBIO(query)(
      setParams = pp => JooqSetParams.setFilter(pp, whereFilter),
      readResult = _.as(readsAsUnit)
    )
    action.map { result =>
      if (result.nonEmpty) {
        // fixme: decide which error to use
        throw RequiredRelationWouldBeViolated(relation)
        //        throw RelationIsRequired(field.name, field.model.name)
      }
    }
  }
  //endregion

  private val dbioUnit                          = DBIO.successful(())
  private val readsAsUnit: ReadsResultSet[Unit] = ReadsResultSet(_ => ())

  private def readId(model: Model) = ReadsResultSet(_.getId(model))
}
