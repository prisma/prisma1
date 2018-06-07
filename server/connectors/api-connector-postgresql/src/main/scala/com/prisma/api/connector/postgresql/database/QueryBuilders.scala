package com.prisma.api.connector.postgresql.database

import java.sql.PreparedStatement

import com.prisma.api.connector._
import com.prisma.slick.NewJdbcExtensions._
import com.prisma.api.connector.postgresql.database.PostgresSlickExtensions._
import com.prisma.gc_values.{IdGCValue, NullGCValue, StringGCValue}
import com.prisma.shared.models._
import slick.jdbc.PositionedParameters

object QueryBuilders {
  val topLevelAlias = "Alias"
}

case class RelationQueryBuilder(schemaName: String, relation: Relation, queryArguments: Option[QueryArguments]) {
  import QueryBuilders.topLevelAlias

  lazy val queryString: String = {
    val tableName = relation.relationTableName
    s"""SELECT * FROM "$schemaName"."$tableName" AS "$topLevelAlias" """ +
      WhereClauseBuilder(schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse("") +
      OrderByClauseBuilder.forRelation(relation, topLevelAlias, queryArguments) +
      LimitClauseBuilder.limitClause(queryArguments)
  }
}

case class CountQueryBuilder(schemaName: String, table: String, filter: Option[Filter]) {
  import QueryBuilders.topLevelAlias

  lazy val queryString: String = {
    s"""SELECT COUNT(*) FROM "$schemaName"."$table" AS "$topLevelAlias" """ +
      WhereClauseBuilder(schemaName).buildWhereClause(filter).getOrElse("")
  }
}

case class ScalarListQueryBuilder(schemaName: String, field: ScalarField, queryArguments: Option[QueryArguments]) {
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

case class RelatedModelsQueryBuilder(
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

  lazy val queryStringWithPagination2: String = {
    s"""SELECT *
        FROM
        ( SELECT ROW_NUMBER() OVER (PARTITION BY "t"."__Relation__A"""" + OrderByClauseBuilder.internal("t", "t", "__Relation__A", queryArguments) + //todo  columnForRelatedModel
      s""") AS "r", "t".*
          FROM (
              SELECT "$topLevelAlias".*, "RelationTable"."$aColumn" AS "__Relation__A",  "RelationTable"."$bColumn" AS "__Relation__B"
              FROM "$schemaName"."$modelTable" AS "$topLevelAlias"
              INNER JOIN "$schemaName"."$relationTableName" AS "RelationTable"
              ON "$topLevelAlias"."${relatedModel.dbNameOfIdField_!}" = "RelationTable"."$fieldRelationSideColumn"
              WHERE "RelationTable"."$modelRelationSideColumn" IN ${queryPlaceHolders(relatedNodeIds)}
              AND """ + WhereClauseBuilder(schemaName).buildWhereClauseWithoutWhereKeyWord(queryArguments.flatMap(_.filter)) +
      WhereClauseBuilder(schemaName).buildCursorCondition(queryArguments, relatedModel).map(" AND " + _).getOrElse("") + s""") AS "t"
       ) AS "x"
       WHERE "x"."r"""" + LimitClauseBuilder.limitClauseForWindowFunction(queryArguments)
  }

//  lazy val queryStringWithPagination: String = {
//    s"""select "$topLevelAlias".*, "RelationTable"."$aColumn" as "__Relation__A",  "RelationTable"."$bColumn" as "__Relation__B"
//            from "$schemaName"."$modelTable" as "$topLevelAlias"
//            inner join "$schemaName"."$relationTableName" as "RelationTable"
//            on "$topLevelAlias"."${relatedModel.dbNameOfIdField_!}" = "RelationTable"."$fieldRelationSideColumn"
//            where "RelationTable"."$modelRelationSideColumn" = ? AND """ +
//      WhereClauseBuilder(schemaName).buildWhereClauseWithoutWhereKeyWord(queryArguments.flatMap(_.filter)) +
//      WhereClauseBuilder(schemaName).buildCursorCondition(queryArguments, relatedModel).map(" AND " + _).getOrElse("") +
//      OrderByClauseBuilder.internal(topLevelAlias, "RelationTable", columnForRelatedModel, queryArguments) +
//      LimitClauseBuilder.limitClause(queryArguments)
//  }

  lazy val queryStringWithoutPagination: String = {
    s"""select "$topLevelAlias".*, "RelationTable"."$aColumn" as "__Relation__A",  "RelationTable"."$bColumn" as "__Relation__B"
            from "$schemaName"."$modelTable" as "$topLevelAlias"
            inner join "$schemaName"."$relationTableName" as "RelationTable"
            on "$topLevelAlias"."${relatedModel.dbNameOfIdField_!}" = "RelationTable"."$fieldRelationSideColumn"
            where "RelationTable"."$modelRelationSideColumn" IN ${queryPlaceHolders(relatedNodeIds)} AND """ +
      WhereClauseBuilder(schemaName).buildWhereClauseWithoutWhereKeyWord(queryArguments.flatMap(_.filter)) +
      OrderByClauseBuilder.internal(topLevelAlias, "RelationTable", columnForRelatedModel, queryArguments)
  }
}

case class ModelQueryBuilder(schemaName: String, model: Model, queryArguments: Option[QueryArguments]) {
  import QueryBuilders.topLevelAlias

  lazy val queryString: String = {
    s"""SELECT * FROM "$schemaName"."${model.dbName}" AS "$topLevelAlias" """ +
      WhereClauseBuilder(schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse("") +
      WhereClauseBuilder(schemaName).buildCursorCondition(queryArguments, model).map(" AND " + _).getOrElse("") +
      OrderByClauseBuilder.forModel(model, topLevelAlias, queryArguments) +
      LimitClauseBuilder.limitClause(queryArguments)
  }

}

object SetParams {
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
      case NodeSubscriptionFilter()                       => // NOOP
      case AndFilter(filters)                             => filters.foreach(setParams(pp, _))
      case OrFilter(filters)                              => filters.foreach(setParams(pp, _))
      case NotFilter(filters)                             => filters.foreach(setParams(pp, _))
      case NodeFilter(filters)                            => setParams(pp, OrFilter(filters))
      case RelationFilter(field, nestedFilter, condition) => setParams(pp, nestedFilter)
      //--------------------------------ANCHORS------------------------------------
      case PreComputedSubscriptionFilter(value)                     => // NOOP
      case ScalarFilter(field, Contains(value: StringGCValue))      => pp.setString("%" + value.value + "%")
      case ScalarFilter(field, NotContains(value: StringGCValue))   => pp.setString("%" + value.value + "%")
      case ScalarFilter(field, StartsWith(value: StringGCValue))    => pp.setString(value.value + "%")
      case ScalarFilter(field, NotStartsWith(value: StringGCValue)) => pp.setString(value.value + "%")
      case ScalarFilter(field, EndsWith(value: StringGCValue))      => pp.setString("%" + value.value)
      case ScalarFilter(field, NotEndsWith(value: StringGCValue))   => pp.setString("%" + value.value)
      case ScalarFilter(field, LessThan(value))                     => pp.setGcValue(value)
      case ScalarFilter(field, GreaterThan(value))                  => pp.setGcValue(value)
      case ScalarFilter(field, LessThanOrEquals(value))             => pp.setGcValue(value)
      case ScalarFilter(field, GreaterThanOrEquals(value))          => pp.setGcValue(value)
      case ScalarFilter(field, NotEquals(NullGCValue))              => // NOOP
      case ScalarFilter(field, NotEquals(value))                    => pp.setGcValue(value)
      case ScalarFilter(field, Equals(NullGCValue))                 => // NOOP
      case ScalarFilter(field, Equals(value))                       => pp.setGcValue(value)
      case ScalarFilter(field, In(Vector(NullGCValue)))             => // NOOP
      case ScalarFilter(field, NotIn(Vector(NullGCValue)))          => // NOOP
      case ScalarFilter(field, In(values))                          => values.foreach(pp.setGcValue)
      case ScalarFilter(field, NotIn(values))                       => values.foreach(pp.setGcValue)
      case OneRelationIsNullFilter(field)                           => // NOOP
      case x                                                        => sys.error(s"Not supported: $x")
    }
  }
}
