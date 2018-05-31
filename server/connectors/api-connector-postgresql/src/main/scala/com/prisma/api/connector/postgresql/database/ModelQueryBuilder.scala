package com.prisma.api.connector.postgresql.database

import java.sql.PreparedStatement

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.PostgresSlickExtensions._
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.{InvalidFirstArgument, InvalidLastArgument, InvalidSkipArgument}
import com.prisma.gc_values.{GCValue, NullGCValue, StringGCValue}
import com.prisma.shared.models.{Field, Model, RelationField, ScalarField}
import slick.jdbc.PositionedParameters

object QueryBuilders {
  def model(schemaName: String, model: Model, args: Option[QueryArguments]): QueryBuilder                      = ModelQueryBuilder(schemaName, model, args)
  def scalarList(schemaName: String, field: ScalarField, queryArguments: Option[QueryArguments]): QueryBuilder = ???
}

trait QueryBuilder {
  def queryString: String
  def setParams(preparedStatement: PreparedStatement, queryArguments: Option[QueryArguments]): Unit
}

case class ModelQueryBuilder(schemaName: String, model: Model, queryArguments: Option[QueryArguments]) extends QueryBuilder {
  val topLevelAlias = "Alias"

  def filter         = queryArguments.flatMap(_.filter)
  def orderBy        = queryArguments.flatMap(_.orderBy)
  def skip           = queryArguments.flatMap(_.skip)
  def before         = queryArguments.flatMap(_.before)
  def after          = queryArguments.flatMap(_.after)
  def first          = queryArguments.flatMap(_.first)
  def last           = queryArguments.flatMap(_.last)
  def isReverseOrder = last.isDefined

  lazy val queryString: String = {
    s"""SELECT * FROM "$schemaName"."${model.dbName}" AS "$topLevelAlias" """ +
      whereClause +
      orderByClause +
      limitClause
  }

  lazy val whereClause = {
    filter match {
      case Some(filter) =>
        val filterConditions = buildWheresForFilter(filter, topLevelAlias)
        if (filterConditions.isEmpty) {
          ""
        } else {
          "WHERE " + filterConditions
        }

      case None =>
        ""
    }
  }

  lazy val limitClause: String = {
    val maxNodeCount = 1000
    (first, last, skip) match {
      case (Some(first), _, _) if first < 0 => throw InvalidFirstArgument()
      case (_, Some(last), _) if last < 0   => throw InvalidLastArgument()
      case (_, _, Some(skip)) if skip < 0   => throw InvalidSkipArgument()
      case _ =>
        val count: Option[Int] = last.isDefined match {
          case true  => last
          case false => first
        }
        // Increase by 1 to know if we have a next page / previous page for relay queries
        val limitedCount: String = count match {
          case None                        => maxNodeCount.toString
          case Some(x) if x > maxNodeCount => throw APIErrors.TooManyNodesRequested(x)
          case Some(x)                     => (x + 1).toString
        }
        s"LIMIT $limitedCount OFFSET ${skip.getOrElse(0)}"
    }
  }

  lazy val orderByClause: String = {
    if (first.isDefined && last.isDefined) throw APIErrors.InvalidConnectionArguments()

    // The limit instruction only works from up to down. Therefore, we have to invert order when we use before.
    val defaultOrder = orderBy.map(_.sortOrder.toString).getOrElse("asc")
    val (order, idOrder) = isReverseOrder match {
      case true  => (invertOrder(defaultOrder), "desc")
      case false => (defaultOrder, "asc")
    }

    val idFieldName = model.dbNameOfIdField_!
    val idField     = s""" "$topLevelAlias"."$idFieldName" """

    orderBy match {
      case Some(orderByArg) if orderByArg.field.name != idFieldName =>
        val orderByField = s""" "$topLevelAlias"."${orderByArg.field.dbName}" """

        // First order by the orderByField, then by id to break ties
        s""" ORDER BY $orderByField $order, $idField $idOrder """

      case _ =>
        // by default, order by id. For performance reasons use the id in the relation table
        s""" ORDER BY $idField $order """
    }
  }

  private def invertOrder(order: String) = order.trim().toLowerCase match {
    case "desc" => "asc"
    case "asc"  => "desc"
    case _      => throw new IllegalArgumentException
  }

  private def buildWheresForFilter(filter: Filter, alias: String): String = {
    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter()                       => ""
      case AndFilter(filters)                             => filters.map(buildWheresForFilter(_, alias)).mkString(" AND ")
      case OrFilter(filters)                              => filters.map(buildWheresForFilter(_, alias)).mkString(" OR ")
      case NotFilter(filters)                             => "NOT " + filters.map(buildWheresForFilter(_, alias)).mkString(" AND NOT ")
      case NodeFilter(filters)                            => buildWheresForFilter(OrFilter(filters), alias)
      case RelationFilter(field, nestedFilter, condition) => relationFilterStatement(alias, field, nestedFilter, condition)
      //--------------------------------ANCHORS------------------------------------
      case PreComputedSubscriptionFilter(value)            => if (value) "TRUE" else "FALSE"
      case ScalarFilter(field, Contains(value))            => column(alias, field) + s""" LIKE ? """
      case ScalarFilter(field, NotContains(value))         => column(alias, field) + s""" NOT LIKE ? """
      case ScalarFilter(field, StartsWith(value))          => column(alias, field) + s""" LIKE ? """
      case ScalarFilter(field, NotStartsWith(value))       => column(alias, field) + s""" NOT LIKE ?"""
      case ScalarFilter(field, EndsWith(value))            => column(alias, field) + s""" LIKE ?"""
      case ScalarFilter(field, NotEndsWith(value))         => column(alias, field) + s""" NOT LIKE ?"""
      case ScalarFilter(field, LessThan(value))            => column(alias, field) ++ s""" < ?"""
      case ScalarFilter(field, GreaterThan(value))         => column(alias, field) ++ s""" > ?"""
      case ScalarFilter(field, LessThanOrEquals(value))    => column(alias, field) ++ s""" <= ?"""
      case ScalarFilter(field, GreaterThanOrEquals(value)) => column(alias, field) ++ s""" >= ?"""
      case ScalarFilter(field, NotEquals(NullGCValue))     => column(alias, field) ++ s""" IS NOT NULL"""
      case ScalarFilter(field, NotEquals(value))           => column(alias, field) ++ s""" != ?"""
      case ScalarFilter(field, Equals(NullGCValue))        => column(alias, field) + s""" IS NULL"""
      case ScalarFilter(field, Equals(value))              => column(alias, field) + s""" = ?"""
      case ScalarFilter(field, In(Vector(NullGCValue)))    => if (field.isRequired) s"false" else column(alias, field) ++ s""" IS NULL"""
      case ScalarFilter(field, NotIn(Vector(NullGCValue))) => if (field.isRequired) s"true" else column(alias, field) ++ s""" IS NOT NULL"""
      case ScalarFilter(field, In(values))                 => if (values.nonEmpty) column(alias, field) ++ in(values) else s"false"
      case ScalarFilter(field, NotIn(values))              => if (values.nonEmpty) column(alias, field) ++ s""" NOT """ ++ in(values) else s"true"
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

  private def relationFilterStatement(alias: String, field: RelationField, nestedFilter: Filter, relationCondition: RelationCondition): String = {
    val relationTableName = field.relation.relationTableName
    val column            = field.relation.columnForRelationSide(field.relationSide)
    val oppositeColumn    = field.relation.columnForRelationSide(field.oppositeRelationSide)

    val newAlias = field.relatedModel_!.dbName + "_" + alias

    val join = s"""select *
            from "$schemaName"."${field.relatedModel_!.dbName}" as "$newAlias"
            inner join "$schemaName"."${relationTableName}"
            on "$newAlias"."${field.relatedModel_!.dbNameOfIdField_!}" = "$schemaName"."${relationTableName}"."${oppositeColumn}"
            where "$schemaName"."${relationTableName}"."${column}" = "$alias"."${field.model.dbNameOfIdField_!}" """

    val nestedFilterStatement = {
      val x = buildWheresForFilter(nestedFilter, newAlias)
      if (x.isEmpty) s"TRUE" else x
    }

    relationCondition match {
      case AtLeastOneRelatedNode => s" exists (" + join + s"and " + nestedFilterStatement + ")"
      case EveryRelatedNode      => s" not exists (" + join + s"and not " + nestedFilterStatement + ")"
      case NoRelatedNode         => s" not exists (" + join + s"and " + nestedFilterStatement + ")"
      case NoRelationCondition   => s" exists (" + join + s"and " + nestedFilterStatement + ")"
    }
  }

  private def column(alias: String, field: Field): String = s""""$alias"."${field.dbName}" """
  private def in(items: Vector[GCValue])                  = s" IN (" + items.map(_ => "?").mkString(",") + ")"

  def setParams(preparedStatement: PreparedStatement, queryArguments: Option[QueryArguments]): Unit = queryArguments.flatMap(_.filter).foreach { filter =>
    setParams(new PositionedParameters(preparedStatement), filter)
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
