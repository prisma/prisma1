package com.prisma.api.connector.postgresql.database

import java.sql.{Connection, PreparedStatement}

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.PostgresSlickExtensions._
import com.prisma.api.connector.postgresql.database.JooqQueryBuilders._
import com.prisma.gc_values.{GCValue, IdGCValue, NullGCValue, StringGCValue}
import com.prisma.shared.models._
import com.prisma.slick.NewJdbcExtensions._
import org.jooq.{Field, _}
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DSL._
import slick.jdbc.PositionedParameters

object JooqQueryBuilders {
  val topLevelAlias      = "Alias"
  val relationTableAlias = "RelationTable"
  val intDummyValue      = 1
  val stringDummy        = ""
  val aSideAlias         = "__Relation__A"
  val bSideAlias         = "__Relation__B"
}

case class JooqRelationQueryBuilder(schemaName: String, relation: Relation, queryArguments: Option[QueryArguments]) {

  lazy val queryString: String = {

    val sql          = DSL.using(SQLDialect.POSTGRES_9_5, new Settings().withRenderFormatted(true))
    val aliasedTable = table(name(schemaName, relation.relationTableName)).as(topLevelAlias)
    val condition    = JooqWhereClauseBuilder(schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse(trueCondition())
    val order        = JooqOrderByClauseBuilder.forRelation(relation, topLevelAlias, queryArguments)
    val limit        = JooqLimitClauseBuilder.limitClause(queryArguments)

    val base = sql
      .select()
      .from(aliasedTable)
      .where(condition)
      .orderBy(order: _*)

    val finalQuery = limit match {
      case Some(_) => base.limit(intDummyValue).offset(intDummyValue)
      case None    => base
    }

    finalQuery.getSQL
  }
}

case class JooqCountQueryBuilder(schemaName: String, tableName: String, filter: Option[Filter]) {

  lazy val queryString: String = {
    val sql          = DSL.using(SQLDialect.POSTGRES_9_5, new Settings().withRenderFormatted(true))
    val aliasedTable = table(name(schemaName, tableName)).as(topLevelAlias)
    val condition    = JooqWhereClauseBuilder(schemaName).buildWhereClause(filter).getOrElse(trueCondition())

    val query = sql
      .selectCount()
      .from(aliasedTable)
      .where(condition)

    query.getSQL
  }
}

case class JooqScalarListQueryBuilder(schemaName: String, field: ScalarField, queryArguments: Option[QueryArguments]) {
  require(field.isList, "This must be called only with scalar list fields")

  val tableName = s"${field.model.dbName}_${field.dbName}"
  lazy val queryString: String = {
    val sql          = DSL.using(SQLDialect.POSTGRES_9_5, new Settings().withRenderFormatted(true))
    val aliasedTable = table(name(schemaName, tableName)).as(topLevelAlias)
    val condition    = JooqWhereClauseBuilder(schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse(trueCondition())
    val order        = JooqOrderByClauseBuilder.forScalarListField(topLevelAlias, queryArguments)
    val limit        = JooqLimitClauseBuilder.limitClause(queryArguments)

    val base = sql
      .select()
      .from(aliasedTable)
      .where(condition)
      .orderBy(order: _*)

    val finalQuery = limit match {
      case Some(_) => base.limit(intDummyValue).offset(intDummyValue)
      case None    => base
    }

    finalQuery.getSQL
  }
}

case class JooqScalarListByUniquesQueryBuilder(schemaName: String, scalarField: ScalarField, nodeIds: Vector[GCValue]) {
  require(scalarField.isList, "This must be called only with scalar list fields")

  val tableName = s"${scalarField.model.dbName}_${scalarField.dbName}"
  lazy val queryString: String = {

    val sql         = DSL.using(SQLDialect.POSTGRES_9_5, new Settings().withRenderFormatted(true))
    val nodeIdField = field(name(schemaName, tableName, "nodeId"))

    val condition = nodeIdField.in(Vector.fill(nodeIds.length) { "" }: _*)
    val query = sql
      .select(nodeIdField, field(name("position")), field(name("value")))
      .from(table(name(schemaName, tableName)))
      .where(condition)

    query.getSQL
  }
}

case class JooqRelatedModelsQueryBuilder(
    schemaName: String,
    fromField: RelationField,
    queryArguments: Option[QueryArguments],
    relatedNodeIds: Vector[IdGCValue]
) {

  val relation                        = fromField.relation
  val relatedModel                    = fromField.relatedModel_!
  val modelTable                      = relatedModel.dbName
  val relationTableName               = fromField.relation.relationTableName
  val modelRelationSideColumn         = relation.columnForRelationSide(fromField.relationSide)
  val oppositeModelRelationSideColumn = relation.columnForRelationSide(fromField.oppositeRelationSide)
  val aColumn                         = relation.modelAColumn
  val bColumn                         = relation.modelBColumn
  val secondaryOrderByForPagination   = if (fromField.oppositeRelationSide == RelationSide.A) "__Relation__A" else "__Relation__B"

  lazy val queryStringWithPagination2: String = {
    s"""SELECT *
        FROM
        ( SELECT ROW_NUMBER() OVER (PARTITION BY "t"."__Relation__A"""" + OrderByClauseBuilder.internal("t", "t", secondaryOrderByForPagination, queryArguments) +
      s""") AS "r", "t".*
          FROM (
              SELECT "$topLevelAlias".*, "RelationTable"."$aColumn" AS "__Relation__A",  "RelationTable"."$bColumn" AS "__Relation__B"
              FROM "$schemaName"."$modelTable" AS "$topLevelAlias"
              INNER JOIN "$schemaName"."$relationTableName" AS "RelationTable"
              ON "$topLevelAlias"."${relatedModel.dbNameOfIdField_!}" = "RelationTable"."$oppositeModelRelationSideColumn"
              WHERE "RelationTable"."$modelRelationSideColumn" IN ${queryPlaceHolders(relatedNodeIds)}
              AND """ + WhereClauseBuilder(schemaName).buildWhereClauseWithoutWhereKeyWord(queryArguments.flatMap(_.filter)) +
      WhereClauseBuilder(schemaName).buildCursorCondition(queryArguments, relatedModel).map(" AND " + _).getOrElse("") + s""") AS "t"
       ) AS "x"
       WHERE "x"."r"""" + LimitClauseBuilder.limitClauseForWindowFunction(queryArguments)
  }

  lazy val queryStringWithoutPagination2: String = {
    s"""select "$topLevelAlias".*, "RelationTable"."$aColumn" as "__Relation__A",  "RelationTable"."$bColumn" as "__Relation__B"
            from "$schemaName"."$modelTable" as "$topLevelAlias"
            inner join "$schemaName"."$relationTableName" as "RelationTable"
            on "$topLevelAlias"."${relatedModel.dbNameOfIdField_!}" = "RelationTable"."$oppositeModelRelationSideColumn"
            where "RelationTable"."$modelRelationSideColumn" IN ${queryPlaceHolders(relatedNodeIds)} AND """ +
      WhereClauseBuilder(schemaName).buildWhereClauseWithoutWhereKeyWord(queryArguments.flatMap(_.filter)) +
      OrderByClauseBuilder.internal(topLevelAlias, "RelationTable", oppositeModelRelationSideColumn, queryArguments)
  }

  val sql           = DSL.using(SQLDialect.POSTGRES_9_5, new Settings().withRenderFormatted(true))
  val aliasedTable  = table(name(schemaName, modelTable)).as(topLevelAlias)
  val relationTable = table(name(schemaName, relationTableName)).as(relationTableAlias)
  val condition1    = field(name(relationTableAlias, modelRelationSideColumn)).in(Vector.fill(relatedNodeIds.length) { stringDummy }: _*)
  val condition2    = JooqWhereClauseBuilder(schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse(trueCondition())

  val base = sql
    .select(aliasedTable.asterisk(), field(name(relationTableAlias, aColumn)).as(aSideAlias),field(name(relationTableAlias, bColumn)).as(bSideAlias) )
    .from(aliasedTable)
    .innerJoin(relationTable)
    .on(field(name(topLevelAlias, relatedModel.dbNameOfIdField_!)).eq(field(name(relationTableAlias, oppositeModelRelationSideColumn))))
    .where(condition1, condition2)

  lazy val queryStringWithPagination: String = {
    val order = JooqOrderByClauseBuilder.internal(topLevelAlias, relationTableAlias, oppositeModelRelationSideColumn, queryArguments)

    val aliasedBase = base.asTable().as("t")

    val rowNumberPart = rowNumber().over().partitionBy(field(name("t", aSideAlias))).orderBy(field(name(aSideAlias))).as("r")

    val withRowNumbers = select(rowNumberPart, aliasedBase.asterisk()).from(aliasedBase).asTable().as("x")

    val withPagination = sql
      .select(withRowNumbers.asterisk())
      .from(withRowNumbers)
      .where()

    withPagination.getSQL
  }

  lazy val queryStringWithoutPagination: String = {
    val order             = JooqOrderByClauseBuilder.internal(topLevelAlias, relationTableAlias, oppositeModelRelationSideColumn, queryArguments) 
    val withoutPagination = base.orderBy(order: _*)
    withoutPagination.getSQL
  }
}

case class JooqModelQueryBuilder(schemaName: String, model: Model, queryArguments: Option[QueryArguments]) {

  lazy val queryString: String = {
    import org.jooq.impl.DSL
    import org.jooq.impl.DSL._

    val sql = DSL.using(SQLDialect.POSTGRES_9_5, new Settings().withRenderFormatted(true))

    val condition       = JooqWhereClauseBuilder(schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse(and(trueCondition()))
    val cursorCondition = JooqWhereClauseBuilder(schemaName).buildCursorCondition(queryArguments, model)
    val order           = JooqOrderByClauseBuilder.forModel(model, topLevelAlias, queryArguments)
    val limit           = JooqLimitClauseBuilder.limitClause(queryArguments)

    val aliasedTable = table(name(schemaName, model.dbName)).as(topLevelAlias)

    val base = sql
      .select()
      .from(aliasedTable)
      .where(condition, cursorCondition)
      .orderBy(order: _*)

    val finalQuery = limit match {
      case Some(_) => base.limit(intDummyValue).offset(intDummyValue)
      case None    => base
    }

    finalQuery.getSQL
  }
}

object JooqSetParams {
  def setQueryArgs(preparedStatement: PreparedStatement, queryArguments: Option[QueryArguments]): Unit = {
    val pp = new PositionedParameters(preparedStatement)
    queryArguments.foreach { queryArgs =>
      setFilter(pp, queryArgs.filter)
    }

    queryArguments.foreach { queryArgs =>
      setCursor(pp, queryArgs)
    }

    queryArguments.foreach { queryArgs =>
      setLimit(pp, queryArgs)
    }

  }

  def setFilter(pp: PositionedParameters, filter: Option[Filter]): Unit = {
    filter.foreach { filter =>
      setParams(pp, filter)
    }
  }

  def setCursor(pp: PositionedParameters, queryArguments: QueryArguments): Unit = {
    queryArguments.after.foreach {
      pp.setString
    }
    queryArguments.after.foreach {
      pp.setString
    }
    queryArguments.before.foreach {
      pp.setString
    }
    queryArguments.before.foreach {
      pp.setString
    }
  }

  def setLimit(pp: PositionedParameters, queryArguments: QueryArguments): Unit = {
    queryArguments.first.foreach { _ =>
      val (first, second) = JooqLimitClauseBuilder.limitClause(Some(queryArguments)).get
      pp.setInt(first)
      pp.setInt(second)
    }

    queryArguments.last.foreach { _ =>
      val (first, second) = JooqLimitClauseBuilder.limitClause(Some(queryArguments)).get
      pp.setInt(first)
      pp.setInt(second)
    }
  }

  def setParams(pp: PositionedParameters, filter: Filter): Unit = {
    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter()           => // NOOP
      case AndFilter(filters)                 => filters.foreach(setParams(pp, _))
      case OrFilter(filters)                  => filters.foreach(setParams(pp, _))
      case NotFilter(filters)                 => filters.foreach(setParams(pp, _))
      case NodeFilter(filters)                => setParams(pp, OrFilter(filters))
      case RelationFilter(_, nestedFilter, _) => setParams(pp, nestedFilter)
      //--------------------------------ANCHORS------------------------------------
      case PreComputedSubscriptionFilter(_)                     => // NOOP
      case ScalarFilter(_, Contains(StringGCValue(value)))      => pp.setString(value)
      case ScalarFilter(_, NotContains(StringGCValue(value)))   => pp.setString(value)
      case ScalarFilter(_, StartsWith(StringGCValue(value)))    => pp.setString(value)
      case ScalarFilter(_, NotStartsWith(StringGCValue(value))) => pp.setString(value)
      case ScalarFilter(_, EndsWith(StringGCValue(value)))      => pp.setString(value)
      case ScalarFilter(_, NotEndsWith(StringGCValue(value)))   => pp.setString(value)
      case ScalarFilter(_, LessThan(value))                     => pp.setGcValue(value)
      case ScalarFilter(_, GreaterThan(value))                  => pp.setGcValue(value)
      case ScalarFilter(_, LessThanOrEquals(value))             => pp.setGcValue(value)
      case ScalarFilter(_, GreaterThanOrEquals(value))          => pp.setGcValue(value)
      case ScalarFilter(_, NotEquals(NullGCValue))              => // NOOP
      case ScalarFilter(_, NotEquals(value))                    => pp.setGcValue(value)
      case ScalarFilter(_, Equals(NullGCValue))                 => // NOOP
      case ScalarFilter(_, Equals(value))                       => pp.setGcValue(value)
      case ScalarFilter(_, In(Vector(NullGCValue)))             => // NOOP
      case ScalarFilter(_, NotIn(Vector(NullGCValue)))          => // NOOP
      case ScalarFilter(_, In(values))                          => values.foreach(pp.setGcValue)
      case ScalarFilter(_, NotIn(values))                       => values.foreach(pp.setGcValue)
      case OneRelationIsNullFilter(_)                           => // NOOP
      case x                                                    => sys.error(s"Not supported: $x")
    }
  }
}
