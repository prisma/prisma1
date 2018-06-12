package com.prisma.api.connector.postgresql.database

import java.sql.{Connection, PreparedStatement}

import com.prisma.api.connector._
import com.prisma.slick.NewJdbcExtensions._
import com.prisma.api.connector.postgresql.database.PostgresSlickExtensions._
import com.prisma.gc_values.{IdGCValue, NullGCValue, StringGCValue}
import com.prisma.shared.models._
import org.jooq.{Condition, Name, SQLDialect}
import slick.jdbc.PositionedParameters

object JooqQueryBuilders {
  val topLevelAlias = "Alias"
}

case class JooqRelationQueryBuilder(schemaName: String, relation: Relation, queryArguments: Option[QueryArguments]) {
  import QueryBuilders.topLevelAlias

  lazy val queryString: String = {
    val tableName = relation.relationTableName
    s"""SELECT * FROM "$schemaName"."$tableName" AS "$topLevelAlias" """ +
      WhereClauseBuilder(schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse("") +
      OrderByClauseBuilder.forRelation(relation, topLevelAlias, queryArguments) +
      LimitClauseBuilder.limitClause(queryArguments)
  }
}

case class JooqCountQueryBuilder(schemaName: String, table: String, filter: Option[Filter]) {
  import QueryBuilders.topLevelAlias

  lazy val queryString: String = {
    s"""SELECT COUNT(*) FROM "$schemaName"."$table" AS "$topLevelAlias" """ +
      WhereClauseBuilder(schemaName).buildWhereClause(filter).getOrElse("")
  }
}

case class JooqScalarListQueryBuilder(schemaName: String, field: ScalarField, queryArguments: Option[QueryArguments]) {
  import QueryBuilders.topLevelAlias
  require(field.isList, "This must be called only with scalar list fields")

  lazy val queryString: String = {
    val tableName = s"${field.model.dbName}_${field.dbName}"
    s"""SELECT * FROM "$schemaName"."$tableName" AS "$topLevelAlias" """ +
      WhereClauseBuilder(schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse("") +
      OrderByClauseBuilder.forScalarListField(field, topLevelAlias, queryArguments) +
      LimitClauseBuilder.limitClause(queryArguments)
  }
}

case class JooqRelatedModelsQueryBuilder(
    schemaName: String,
    fromField: RelationField,
    queryArguments: Option[QueryArguments],
    relatedNodeIds: Vector[IdGCValue]
) {
  import QueryBuilders.topLevelAlias

  val relation                = fromField.relation
  val relatedModel            = fromField.relatedModel_!
  val modelTable              = relatedModel.dbName
  val relationTableName       = fromField.relation.relationTableName
  val modelRelationSideColumn = relation.columnForRelationSide(fromField.relationSide)
  val fieldRelationSideColumn = relation.columnForRelationSide(fromField.oppositeRelationSide)
  val aColumn                 = relation.modelAColumn
  val bColumn                 = relation.modelBColumn
  val columnForRelatedModel   = relation.columnForRelationSide(fromField.oppositeRelationSide)

  lazy val queryStringWithPagination: String = {
    s"""select "$topLevelAlias".*, "RelationTable"."$aColumn" as "__Relation__A",  "RelationTable"."$bColumn" as "__Relation__B"
            from "$schemaName"."$modelTable" as "$topLevelAlias"
            inner join "$schemaName"."$relationTableName" as "RelationTable"
            on "$topLevelAlias"."${relatedModel.dbNameOfIdField_!}" = "RelationTable"."$fieldRelationSideColumn"
            where "RelationTable"."$modelRelationSideColumn" = ? AND """ +
      WhereClauseBuilder(schemaName).buildWhereClauseWithoutWhereKeyWord(queryArguments.flatMap(_.filter)) +
      WhereClauseBuilder(schemaName).buildCursorCondition(queryArguments, relatedModel).map(" AND " + _).getOrElse("") +
      //      OrderByClauseBuilder.internal("RelationTable", columnForRelatedModel, queryArguments) +
      LimitClauseBuilder.limitClause(queryArguments)
  }

  lazy val queryStringWithoutPagination: String = {
    s"""select "$topLevelAlias".*, "RelationTable"."$aColumn" as "__Relation__A",  "RelationTable"."$bColumn" as "__Relation__B"
            from "$schemaName"."$modelTable" as "$topLevelAlias"
            inner join "$schemaName"."$relationTableName" as "RelationTable"
            on "$topLevelAlias"."${relatedModel.dbNameOfIdField_!}" = "RelationTable"."$fieldRelationSideColumn"
            where "RelationTable"."$modelRelationSideColumn" IN ${queryPlaceHolders(relatedNodeIds)} AND """ +
      WhereClauseBuilder(schemaName).buildWhereClauseWithoutWhereKeyWord(queryArguments.flatMap(_.filter)) // +
    //      OrderByClauseBuilder.internal(alias = "RelationTable", columnForRelatedModel, queryArguments)
  }
}

case class JooqModelQueryBuilder(connection: Connection, schemaName: String, model: Model, queryArguments: Option[QueryArguments]) {
  import QueryBuilders.topLevelAlias

  //  lazy val queryString2: String = {
  //    s"""SELECT * FROM "$schemaName"."${model.dbName}" AS "$topLevelAlias" """ +
  //      JooqWhereClauseBuilder(schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse("") +
  //      WhereClauseBuilder(schemaName).buildCursorCondition(queryArguments, model).map(" AND " + _).getOrElse("") +
  //      OrderByClauseBuilder.forModel(model, topLevelAlias, queryArguments) +
  //      LimitClauseBuilder.limitClause(queryArguments)
  //  }

  lazy val queryString: String = {
    import org.jooq.impl.DSL
    import org.jooq.impl.DSL._

    val sql = DSL.using(connection, SQLDialect.POSTGRES_9_5)

    val conditions: Vector[Condition] = JooqWhereClauseBuilder(connection: Connection, schemaName).buildWhereClause(queryArguments.flatMap(_.filter))

    val aliasedTable = table(name(schemaName, model.dbName)).as(topLevelAlias)

    val string = sql
      .select()
      .from(aliasedTable)
      .where(conditions: _*)

    string.getSQL
  }

}

object JooqSetParams {
  def setQueryArgs(preparedStatement: PreparedStatement, queryArguments: Option[QueryArguments]): Unit = {
    queryArguments.foreach { queryArgs =>
      setFilter(preparedStatement, queryArgs.filter)
    }
  }

  def setFilter(preparedStatement: PreparedStatement, filter: Option[Filter]): Unit = {
    filter.foreach { filter =>
      setParams(new PositionedParameters(preparedStatement), filter)
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
      case ScalarFilter(_, Contains(value: StringGCValue))      => pp.setString("%" + value.value + "%")
      case ScalarFilter(_, NotContains(value: StringGCValue))   => pp.setString("%" + value.value + "%")
      case ScalarFilter(_, StartsWith(value: StringGCValue))    => pp.setString(value.value + "%")
      case ScalarFilter(_, NotStartsWith(value: StringGCValue)) => pp.setString(value.value + "%")
      case ScalarFilter(_, EndsWith(value: StringGCValue))      => pp.setString("%" + value.value)
      case ScalarFilter(_, NotEndsWith(value: StringGCValue))   => pp.setString("%" + value.value)
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
