package com.prisma.api.connector.postgresql.database

import java.sql.PreparedStatement

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.PostgresSlickExtensions._
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.{InvalidFirstArgument, InvalidLastArgument, InvalidSkipArgument}
import com.prisma.gc_values.{GCValue, NullGCValue, StringGCValue}
import com.prisma.shared.models._
import slick.jdbc.{PositionedParameters, SQLActionBuilder}

object QueryBuilders {
  def model(schemaName: String, model: Model, args: Option[QueryArguments]): QueryBuilder            = ModelQueryBuilder(schemaName, model, args)
  def relation(schemaName: String, relation: Relation, args: Option[QueryArguments]): QueryBuilder   = RelationQueryBuilder(schemaName, relation, args)
  def scalarList(schemaName: String, field: ScalarField, args: Option[QueryArguments]): QueryBuilder = ScalarListQueryBuilder(schemaName, field, args)
  def count(schemaName: String, table: String, filter: Option[Filter]): QueryBuilder                 = CountQueryBuilder(schemaName, table, filter)

  val topLevelAlias = "Alias"
}

trait QueryBuilder {
  def queryString: String
  def setParamsForQueryArgs(preparedStatement: PreparedStatement, queryArguments: Option[QueryArguments]): Unit = {
    SetParams.setQueryArgs(preparedStatement, queryArguments)
  }

  def setParamsForFilter(preparedStatement: PreparedStatement, filter: Option[Filter]): Unit = {
    SetParams.setFilter(preparedStatement, filter)
  }
}

case class RelationQueryBuilder(schemaName: String, relation: Relation, queryArguments: Option[QueryArguments]) extends QueryBuilder {
  val topLevelAlias = "Alias"

  lazy val queryString: String = {
    val tableName = relation.relationTableName
    s"""SELECT * FROM "$schemaName"."$tableName" AS "$topLevelAlias" """ +
      WhereClauseBuilder(schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse("") +
      OrderByClauseBuilder.forRelation(relation, topLevelAlias, queryArguments) +
      LimitClauseBuilder.limitClause(queryArguments)
  }
}

case class CountQueryBuilder(schemaName: String, table: String, filter: Option[Filter]) extends QueryBuilder {
  val topLevelAlias = "Alias"

  lazy val queryString: String = {
    s"""SELECT COUNT(*) FROM "$schemaName"."$table" AS "$topLevelAlias" """ +
      WhereClauseBuilder(schemaName).buildWhereClause(filter).getOrElse("")
  }
}

case class ScalarListQueryBuilder(schemaName: String, field: ScalarField, queryArguments: Option[QueryArguments]) extends QueryBuilder {
  require(field.isList, "This must be called only with scalar list fields")
  val topLevelAlias = "Alias"

  lazy val queryString: String = {
    val tableName = s"${field.model.dbName}_${field.dbName}"
    s"""SELECT * FROM "$schemaName"."$tableName" AS "$topLevelAlias" """ +
      WhereClauseBuilder(schemaName).buildWhereClause(queryArguments.flatMap(_.filter)).getOrElse("") +
      OrderByClauseBuilder.forScalarListField(field, topLevelAlias, queryArguments) +
      LimitClauseBuilder.limitClause(queryArguments)
  }
}

//case class CountRelatedModelsQueryBuilder(schemaName: String, fromField: RelationField, queryArguments: Option[QueryArguments]){
//
//}

case class RelatedModelsQueryBuilder(schemaName: String, fromField: RelationField, queryArguments: Option[QueryArguments]) extends QueryBuilder {
  val ALIAS                   = "Alias"
  val relation                = fromField.relation
  val modelRelationSideColumn = relation.columnForRelationSide(fromField.relationSide)
  val fieldRelationSideColumn = relation.columnForRelationSide(fromField.oppositeRelationSide)

  lazy val queryString: String = buildQuery(modelRelationSideColumn, fieldRelationSideColumn)
  val queryStringFromOtherSide = buildQuery(fieldRelationSideColumn, modelRelationSideColumn)

  private def buildQuery(modelRelationSideColumn: String, fieldRelationSideColumn: String): String = {
    val relation              = fromField.relation
    val relatedModel          = fromField.relatedModel_!
    val modelTable            = relatedModel.dbName
    val relationTableName     = fromField.relation.relationTableName
    val aColumn               = relation.modelAColumn
    val bColumn               = relation.modelBColumn
    val columnForRelatedModel = relation.columnForRelationSide(fromField.oppositeRelationSide)

    s"""select "$ALIAS".*, "RelationTable"."$aColumn" as "__Relation__A",  "RelationTable"."$bColumn" as "__Relation__B"
            from "$schemaName"."$modelTable" as "$ALIAS"
            inner join "$schemaName"."$relationTableName" as "RelationTable"
            on "$ALIAS"."${relatedModel.dbNameOfIdField_!}" = "RelationTable"."$fieldRelationSideColumn"
            where "RelationTable"."$modelRelationSideColumn" = ? AND """ +
      WhereClauseBuilder(schemaName).buildWhereClauseWithoutWhereKeyWord(queryArguments.flatMap(_.filter)) +
      WhereClauseBuilder(schemaName).buildCursorCondition(queryArguments, relatedModel).map(" AND " + _).getOrElse("") +
      OrderByClauseBuilder.internal("RelationTable", columnForRelatedModel, queryArguments) +
      LimitClauseBuilder.limitClause(queryArguments)
  }
}

case class ModelQueryBuilder(schemaName: String, model: Model, queryArguments: Option[QueryArguments]) extends QueryBuilder {
  val topLevelAlias = "Alias"

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
