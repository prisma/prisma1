package com.prisma.api.connector.postgresql.database

import java.sql.PreparedStatement

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.JooqQueryBuilders._
import com.prisma.api.connector.postgresql.database.PostgresSlickExtensions._
import com.prisma.gc_values.{GCValue, IdGCValue, NullGCValue, StringGCValue}
import com.prisma.shared.models._
import org.jooq._
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DSL._
import slick.jdbc.PositionedParameters

object JooqQueryBuilders {
  val topLevelAlias       = "Alias"
  val relationTableAlias  = "RelationTable"
  val intDummy            = 1
  val stringDummy         = ""
  val aSideAlias          = "__Relation__A"
  val bSideAlias          = "__Relation__B"
  val rowNumberAlias      = "prismaRowNumberAlias"
  val baseTableAlias      = "prismaBaseTableAlias"
  val rowNumberTableAlias = "prismaRowNumberTableAlias"
  val nodeIdFieldName     = "nodeId"
  val positionFieldName   = "position"
  val valueFieldName      = "value"
  val placeHolder         = "?"
  val relayTableName      = "_RelayId"
  val updatedAtField      = "updatedAt"
  val createdAtField      = "createdAt"
}

case class JooqRelationQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    relation: Relation,
    queryArguments: Option[QueryArguments]
) extends BuilderBase {

  lazy val queryString: String = {
    val aliasedTable = table(name(schemaName, relation.relationTableName)).as(topLevelAlias)
    val condition    = JooqWhereClauseBuilder(slickDatabase, schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse(trueCondition())
    val order        = JooqOrderByClauseBuilder.forRelation(relation, topLevelAlias, queryArguments)
    val limit        = JooqLimitClauseBuilder.limitClause(queryArguments)

    val base = sql
      .select()
      .from(aliasedTable)
      .where(condition)
      .orderBy(order: _*)

    val finalQuery = limit match {
      case Some(_) => base.limit(intDummy).offset(intDummy)
      case None    => base
    }

    finalQuery.getSQL
  }
}

case class JooqCountQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    tableName: String,
    filter: Option[Filter]
) extends BuilderBase {

  lazy val queryString: String = {
    val aliasedTable = table(name(schemaName, tableName)).as(topLevelAlias)
    val condition    = JooqWhereClauseBuilder(slickDatabase, schemaName).buildWhereClause(filter).getOrElse(trueCondition())

    val query = sql
      .selectCount()
      .from(aliasedTable)
      .where(condition)

    query.getSQL
  }
}

case class JooqScalarListQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    field: ScalarField,
    queryArguments: Option[QueryArguments]
) extends BuilderBase {
  require(field.isList, "This must be called only with scalar list fields")

  val tableName = s"${field.model.dbName}_${field.dbName}"
  lazy val queryString: String = {
    val aliasedTable = table(name(schemaName, tableName)).as(topLevelAlias)
    val condition    = JooqWhereClauseBuilder(slickDatabase, schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse(trueCondition())
    val order        = JooqOrderByClauseBuilder.forScalarListField(topLevelAlias, queryArguments)
    val limit        = JooqLimitClauseBuilder.limitClause(queryArguments)

    val base = sql
      .select()
      .from(aliasedTable)
      .where(condition)
      .orderBy(order: _*)

    val finalQuery = limit match {
      case Some(_) => base.limit(intDummy).offset(intDummy)
      case None    => base
    }

    finalQuery.getSQL
  }
}

case class JooqScalarListByUniquesQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    scalarField: ScalarField,
    nodeIds: Vector[GCValue]
) extends BuilderBase {
  require(scalarField.isList, "This must be called only with scalar list fields")

  val tableName = s"${scalarField.model.dbName}_${scalarField.dbName}"
  lazy val queryString: String = {
    val nodeIdField = field(name(schemaName, tableName, nodeIdFieldName))

    val condition = nodeIdField.in(Vector.fill(nodeIds.length) { stringDummy }: _*)
    val query = sql
      .select(nodeIdField, field(name(positionFieldName)), field(name(valueFieldName)))
      .from(table(name(schemaName, tableName)))
      .where(condition)

    query.getSQL
  }
}

case class JooqRelatedModelsQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    fromField: RelationField,
    queryArguments: Option[QueryArguments],
    relatedNodeIds: Vector[IdGCValue]
) extends BuilderBase {

  val relation                        = fromField.relation
  val relatedModel                    = fromField.relatedModel_!
  val modelTable                      = relatedModel.dbName
  val relationTableName               = fromField.relation.relationTableName
  val modelRelationSideColumn         = relation.columnForRelationSide(fromField.relationSide)
  val oppositeModelRelationSideColumn = relation.columnForRelationSide(fromField.oppositeRelationSide)
  val aColumn                         = relation.modelAColumn
  val bColumn                         = relation.modelBColumn
  val secondaryOrderByForPagination   = if (fromField.oppositeRelationSide == RelationSide.A) aSideAlias else bSideAlias

  val aliasedTable            = table(name(schemaName, modelTable)).as(topLevelAlias)
  val relationTable           = table(name(schemaName, relationTableName)).as(relationTableAlias)
  val relatedNodesCondition   = field(name(relationTableAlias, modelRelationSideColumn)).in(placeHolders(relatedNodeIds))
  val queryArgumentsCondition = JooqWhereClauseBuilder(slickDatabase, schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse(trueCondition())

  val base = sql
    .select(aliasedTable.asterisk(), field(name(relationTableAlias, aColumn)).as(aSideAlias), field(name(relationTableAlias, bColumn)).as(bSideAlias))
    .from(aliasedTable)
    .innerJoin(relationTable)
    .on(aliasColumn(relatedModel.dbNameOfIdField_!).eq(field(name(relationTableAlias, oppositeModelRelationSideColumn))))

  lazy val queryStringWithPagination: String = {
    val order           = JooqOrderByClauseBuilder.internal(baseTableAlias, baseTableAlias, secondaryOrderByForPagination, queryArguments)
    val cursorCondition = JooqWhereClauseBuilder(slickDatabase, schemaName).buildCursorCondition(queryArguments, relatedModel)

    val aliasedBase = base.where(relatedNodesCondition, queryArgumentsCondition, cursorCondition).asTable().as(baseTableAlias)

    val rowNumberPart = rowNumber().over().partitionBy(aliasedBase.field(aSideAlias)).orderBy(order: _*).as(rowNumberAlias)

    val withRowNumbers = select(rowNumberPart, aliasedBase.asterisk()).from(aliasedBase).asTable().as(rowNumberTableAlias)

    val limitCondition = rowNumberPart.between(intDummy).and(intDummy)

    val withPagination = sql
      .select(withRowNumbers.asterisk())
      .from(withRowNumbers)
      .where(limitCondition)

    withPagination.getSQL
  }

  lazy val mysqlHack = {
    val relatedNodeCondition = field(name(relationTableAlias, modelRelationSideColumn)).equal(placeHolder)
    val order                = JooqOrderByClauseBuilder.internal2(secondaryOrderByForPagination, queryArguments)
    val cursorCondition      = JooqWhereClauseBuilder(slickDatabase, schemaName).buildCursorCondition(queryArguments, relatedModel)

    val singleQuery =
      base
        .where(relatedNodeCondition, queryArgumentsCondition, cursorCondition)
        .orderBy(order: _*)
        .limit(intDummy)
        .offset(intDummy)

    singleQuery
  }

  lazy val queryStringWithoutPagination: String = {
    val order = JooqOrderByClauseBuilder.internal(topLevelAlias, relationTableAlias, oppositeModelRelationSideColumn, queryArguments)
    val withoutPagination = base
      .where(relatedNodesCondition, queryArgumentsCondition)
      .orderBy(order: _*)

    withoutPagination.getSQL
  }
}

case class JooqModelQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    model: Model,
    queryArguments: Option[QueryArguments]
) extends BuilderBase {

  lazy val queryString: String = {
    val condition       = JooqWhereClauseBuilder(slickDatabase, schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse(and(trueCondition()))
    val cursorCondition = JooqWhereClauseBuilder(slickDatabase, schemaName).buildCursorCondition(queryArguments, model)
    val order           = JooqOrderByClauseBuilder.forModel(model, topLevelAlias, queryArguments)
    val limit           = JooqLimitClauseBuilder.limitClause(queryArguments)

    val aliasedTable = table(name(schemaName, model.dbName)).as(topLevelAlias)

    val base = sql
      .select()
      .from(aliasedTable)
      .where(condition, cursorCondition)
      .orderBy(order: _*)

    val finalQuery = limit match {
      case Some(_) => base.limit(intDummy).offset(intDummy)
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
      setCursor(pp, queryArgs)
      setLimit(pp, queryArgs)
    }
  }

  def setFilter(pp: PositionedParameters, filter: Option[Filter]): Unit = {
    filter.foreach { filter =>
      setParams(pp, filter)
    }
  }

  def setCursor(pp: PositionedParameters, queryArguments: QueryArguments): Unit = {
    queryArguments.after.foreach { value =>
      pp.setString(value)
      pp.setString(value)
    }
    queryArguments.before.foreach { value =>
      pp.setString(value)
      pp.setString(value)
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
