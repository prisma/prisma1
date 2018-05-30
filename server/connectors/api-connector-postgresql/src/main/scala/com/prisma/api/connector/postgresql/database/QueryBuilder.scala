package com.prisma.api.connector.postgresql.database

import java.sql.PreparedStatement

import com.prisma.api.connector._
import com.prisma.gc_values.{GCValue, GCValueExtractor, NullGCValue, StringGCValue}
import com.prisma.shared.models.{Field, Model, RelationField}
import slick.jdbc.{PositionedParameters, SQLActionBuilder}
import slick.jdbc.PostgresProfile.api._
import PostgresSlickExtensions._
import JdbcExtensions._
import com.prisma.api.connector.postgresql.database.PostgresQueryArgumentsHelpers.{generateFilterConditions, in}

object QueryDsl {
  def select(schemaName: String, model: Model): QueryBuilder = QueryBuilder(schemaName, model, Vector.empty, None)

}

case class QueryBuilderWhere(field: Field, value: GCValue)

case class QueryBuilder(schemaName: String, model: Model, wheres: Vector[QueryBuilderWhere], filter: Option[Filter]) {
  val alias = PostgresQueryArgumentsExtensions.ALIAS

  def where(filter: Option[Filter]): QueryBuilder = copy(filter = filter)

  def build(): String = s"""SELECT * FROM "${schemaName}"."${model.dbName}" AS "$alias" """ + buildWhereClause()

  private def buildWhereClause() = {
    filter match {
      case Some(filter) =>
        val filterConditions = buildWheresForFilter(filter)
        if (filterConditions.isEmpty) {
          ""
        } else {
          "WHERE " + filterConditions
        }

      case None =>
        ""
    }
  }

  private def buildWheresForFilter(filter: Filter): String = {
    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter()                       => ""
      case AndFilter(filters)                             => filters.map(buildWheresForFilter).mkString(" AND ")
      case OrFilter(filters)                              => filters.map(buildWheresForFilter).mkString(" OR ")
      case NotFilter(filters)                             => "NOT " + filters.map(buildWheresForFilter).mkString(" AND NOT ")
      case NodeFilter(filters)                            => buildWheresForFilter(OrFilter(filters))
      case RelationFilter(field, nestedFilter, condition) => relationFilterStatement(field, nestedFilter, condition)
      //--------------------------------ANCHORS------------------------------------
      case PreComputedSubscriptionFilter(value)            => if (value) "TRUE" else "FALSE"
      case ScalarFilter(field, Contains(value))            => valueOf(field) + s""" LIKE ? """
      case ScalarFilter(field, NotContains(value))         => valueOf(field) + s""" NOT LIKE ? """
      case ScalarFilter(field, StartsWith(value))          => valueOf(field) + s""" LIKE ? """
      case ScalarFilter(field, NotStartsWith(value))       => valueOf(field) + s""" NOT LIKE ?"""
      case ScalarFilter(field, EndsWith(value))            => valueOf(field) + s""" LIKE ?"""
      case ScalarFilter(field, NotEndsWith(value))         => valueOf(field) + s""" NOT LIKE ?"""
      case ScalarFilter(field, LessThan(value))            => valueOf(field) ++ s""" < ?"""
      case ScalarFilter(field, GreaterThan(value))         => valueOf(field) ++ s""" > ?"""
      case ScalarFilter(field, LessThanOrEquals(value))    => valueOf(field) ++ s""" <= ?"""
      case ScalarFilter(field, GreaterThanOrEquals(value)) => valueOf(field) ++ s""" >= ?"""
      case ScalarFilter(field, NotEquals(NullGCValue))     => valueOf(field) ++ s""" IS NOT NULL"""
      case ScalarFilter(field, NotEquals(value))           => valueOf(field) ++ s""" != ?"""
      case ScalarFilter(field, Equals(NullGCValue))        => valueOf(field) + s""" IS NULL"""
      case ScalarFilter(field, Equals(value))              => valueOf(field) + s""" = ?"""
      case ScalarFilter(field, In(Vector(NullGCValue)))    => if (field.isRequired) s"false" else valueOf(field) ++ s""" IS NULL"""
      case ScalarFilter(field, NotIn(Vector(NullGCValue))) => if (field.isRequired) s"true" else valueOf(field) ++ s""" IS NOT NULL"""
      case ScalarFilter(field, In(values))                 => if (values.nonEmpty) valueOf(field) ++ in(values) else s"false"
      case ScalarFilter(field, NotIn(values))              => if (values.nonEmpty) valueOf(field) ++ s""" NOT """ ++ in(values) else s"true"
      case OneRelationIsNullFilter(field) =>
        val relation          = field.relation
        val relationTableName = relation.relationTableName
        val column            = relation.columnForRelationSide(field.relationSide)
        val otherIdColumn     = field.relatedModel_!.dbNameOfIdField_!

        s""" not exists (select  *
                from    "$schemaName"."$relationTableName"
                where   "$schemaName"."$relationTableName"."$column" = "$alias"."$otherIdColumn"
              )"""
      case x => sys.error(s"Not supported: $x")
    }
  }

  private def relationFilterStatement(field: RelationField, nestedFilter: Filter, relationCondition: RelationCondition): String = {
    val (alias, modTableName) = getAliasAndTableName(field.model.name, field.relatedModel_!.name)

    val relationTableName = field.relation.relationTableName
    val column            = field.relation.columnForRelationSide(field.relationSide)
    val oppositeColumn    = field.relation.columnForRelationSide(field.oppositeRelationSide)

    val join = s"""select *
            from "$schemaName"."${field.relatedModel_!.dbName}" as "$alias"
            inner join "$schemaName"."${relationTableName}"
            on "$alias"."${field.relatedModel_!.dbNameOfIdField_!}" = "$schemaName"."${relationTableName}"."${oppositeColumn}"
            where "$schemaName"."${relationTableName}"."${column}" = "$modTableName"."${field.model.dbNameOfIdField_!}" """

    val nestedFilterStatement = {
      val x = buildWheresForFilter(nestedFilter)
      if (x.isEmpty) s"TRUE" else x
    }

    relationCondition match {
      case AtLeastOneRelatedNode => s" exists (" + join + s"and " + nestedFilterStatement + ")"
      case EveryRelatedNode      => s" not exists (" + join + s"and not " + nestedFilterStatement + ")"
      case NoRelatedNode         => s" not exists (" + join + s"and " + nestedFilterStatement + ")"
      case NoRelationCondition   => s" exists (" + join + s"and " + nestedFilterStatement + ")"
    }
  }

  private def getAliasAndTableName(fromModel: String, toModel: String): (String, String) = {
    val modTableName = if (!alias.contains("_")) schemaName + """"."""" + fromModel else alias
    val newAlias     = toModel + "_" + alias
    (newAlias, modTableName)
  }

  private def valueOf(field: Field): String = s""""${field.dbName}" """
  private def in(items: Vector[GCValue])    = s" IN (" + items.map(_ => "?").mkString(",") + ")"

  def setParams(preparedStatement: PreparedStatement): Unit = filter.foreach { filter =>
    setParams(new PositionedParameters(preparedStatement), filter)
  }

  private def setParams(pp: PositionedParameters, filter: Filter): Unit = {
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
