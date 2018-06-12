package com.prisma.api.connector.postgresql.database

import java.sql.Connection

import com.prisma.api.connector._
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.{InvalidFirstArgument, InvalidLastArgument, InvalidSkipArgument}
import com.prisma.gc_values.{GCValue, NullGCValue}
import com.prisma.shared.models._
import org.apache.commons.lang.ObjectUtils.Null
import org.jooq.{Condition, SQLDialect}
import org.jooq._
import org.jooq.impl._
import org.jooq.impl.DSL.{field, _}

import collection.JavaConverters._
import org.jooq.scalaextensions.Conversions._

case class JooqWhereClauseBuilder(connection: Connection, schemaName: String) {
  val topLevelAlias: String = QueryBuilders.topLevelAlias
  val sql                   = DSL.using(connection, SQLDialect.POSTGRES_9_5)

  def buildWhereClause(filter: Option[Filter]): Option[Condition] = filter match {
    case Some(filter) => Some(buildWheresForFilter(filter, topLevelAlias))
    case None         => None
  }

  // This creates a query that checks if the id is in a certain set returned by a subquery Q.
  // The subquery Q fetches all the ID's defined by the cursors and order.
  // On invalid cursor params, no error is thrown. The result set will just be empty.

  def buildCursorCondition(queryArguments: Option[QueryArguments], model: Model): Option[String] = {
    for {
      args   <- queryArguments
      result <- buildCursorCondition(args, model)
    } yield result
  }

  def buildCursorCondition(queryArguments: QueryArguments, model: Model): Option[String] = {
    val (before, after, orderBy) = (queryArguments.before, queryArguments.after, queryArguments.orderBy)
    // If both params are empty, don't generate any query.
    if (before.isEmpty && after.isEmpty) return None

    val tableName        = model.dbName
    val idFieldWithAlias = s""""$topLevelAlias"."${model.dbNameOfIdField_!}""""
    val idField          = s""""$schemaName"."$tableName"."${model.dbNameOfIdField_!}""""

    // First, we fetch the ordering for the query. If none is passed, we order by id, ascending.
    // We need that since before/after are dependent on the order.
    val (orderByField, orderByFieldWithAlias, sortDirection) = orderBy match {
      case Some(orderByArg) =>
        (s""""$schemaName"."$tableName"."${orderByArg.field.dbName}"""", s""""$topLevelAlias"."${orderByArg.field.dbName}"""", orderByArg.sortOrder.toString)
      case None => (idField, idFieldWithAlias, "asc")
    }

    // Then, we select the comparison operation and construct the cursors. For instance, if we use ascending order, and we want
    // to get the items before, we use the "<" comparator on the column that defines the order.
    def cursorFor(cursor: String, cursorType: String): String = {
      val compOperator = (cursorType, sortDirection.toLowerCase.trim) match {
        case ("before", "asc")  => "<"
        case ("before", "desc") => ">"
        case ("after", "asc")   => ">"
        case ("after", "desc")  => "<"
        case _                  => throw new IllegalArgumentException
      }

      s"""($orderByFieldWithAlias, $idFieldWithAlias) $compOperator ((select $orderByField from "$schemaName"."$tableName" where $idField = '$cursor'), '$cursor')"""
    }

    val afterCursorFilter  = after.map(cursorFor(_, "after"))
    val beforeCursorFilter = before.map(cursorFor(_, "before"))

    Some((afterCursorFilter ++ beforeCursorFilter).mkString(" AND "))
  }

  private def buildWheresForFilter(filter: Filter, alias: String): Condition = {
    def oneRelationIsNullFilter(relationField: RelationField): Condition = {
      val relation          = relationField.relation
      val relationTableName = relation.relationTableName
      val column            = relation.columnForRelationSide(relationField.relationSide)
      val otherIdColumn     = relationField.relatedModel_!.dbNameOfIdField_!

      val select = sql
        .select()
        .from(name(schemaName, relationTableName))
        .where(field(name(schemaName, relationTableName, column)).eq(field(name(alias, otherIdColumn))))

      trueCondition().andNotExists(select)
    }

    def relationFilterStatement(alias: String, relationFilter: RelationFilter): Condition = {
      val relationField     = relationFilter.field
      val relationTableName = relationField.relation.relationTableName
      val column            = relationField.relation.columnForRelationSide(relationField.relationSide)
      val oppositeColumn    = relationField.relation.columnForRelationSide(relationField.oppositeRelationSide)
      val newAlias          = relationField.relatedModel_!.dbName + "_" + alias

      val select = sql
        .select()
        .from(table(name(schemaName, relationField.relatedModel_!.dbName)).as(newAlias))
        .innerJoin(name(schemaName, relationTableName))
        .on(field(name(newAlias, relationField.relatedModel_!.dbNameOfIdField_!)).eq(field(name(schemaName, relationTableName, oppositeColumn))))
        .where(field(name(schemaName, relationTableName, column)).eq(field(name(alias, relationField.model.dbNameOfIdField_!))))

      trueCondition().andNotExists(select)

      val nestedFilterStatement = buildWheresForFilter(relationFilter.nestedFilter, newAlias)

      relationFilter.condition match {
        case AtLeastOneRelatedNode => trueCondition().andExists(select.and(nestedFilterStatement))
        case EveryRelatedNode      => trueCondition().andNotExists(select.andNot(nestedFilterStatement))
        case NoRelatedNode         => trueCondition().andNotExists(select.and(nestedFilterStatement))
        case NoRelationCondition   => trueCondition().andExists(select.and(nestedFilterStatement))
      }
    }

    def fieldFrom(scalarField: ScalarField) = field(name(alias, scalarField.dbName))

    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter() => and(trueCondition())
      case AndFilter(filters)       => filters.map(buildWheresForFilter(_, alias)).foldLeft(and(trueCondition()))(_ and _)
      case OrFilter(filters)        => filters.map(buildWheresForFilter(_, alias)).foldLeft(and(falseCondition()))(_ or _)
      case NotFilter(filters)       => filters.map(buildWheresForFilter(_, alias)).foldLeft(and(trueCondition()))(_ andNot _)
      case NodeFilter(filters)      => buildWheresForFilter(OrFilter(filters), alias)
      case x: RelationFilter        => relationFilterStatement(alias, x)
      //--------------------------------ANCHORS------------------------------------
      case PreComputedSubscriptionFilter(value)                  => if (value) trueCondition() else falseCondition()
      case ScalarFilter(scalarField, Contains(_))                => fieldFrom(scalarField).contains("")
      case ScalarFilter(scalarField, NotContains(_))             => fieldFrom(scalarField).notContains("")
      case ScalarFilter(scalarField, StartsWith(_))              => fieldFrom(scalarField).startsWith("")
      case ScalarFilter(scalarField, NotStartsWith(_))           => fieldFrom(scalarField).startsWith("").not()
      case ScalarFilter(scalarField, EndsWith(_))                => fieldFrom(scalarField).endsWith("")
      case ScalarFilter(scalarField, NotEndsWith(_))             => fieldFrom(scalarField).endsWith("").not()
      case ScalarFilter(scalarField, LessThan(_))                => fieldFrom(scalarField).lessThan("")
      case ScalarFilter(scalarField, GreaterThan(_))             => fieldFrom(scalarField).greaterThan("")
      case ScalarFilter(scalarField, LessThanOrEquals(_))        => fieldFrom(scalarField).lessOrEqual("")
      case ScalarFilter(scalarField, GreaterThanOrEquals(_))     => fieldFrom(scalarField).greaterOrEqual("")
      case ScalarFilter(scalarField, NotEquals(NullGCValue))     => fieldFrom(scalarField).isNotNull
      case ScalarFilter(scalarField, NotEquals(_))               => fieldFrom(scalarField).notEqual("")
      case ScalarFilter(scalarField, Equals(NullGCValue))        => fieldFrom(scalarField).isNull
      case ScalarFilter(scalarField, Equals(x))                  => fieldFrom(scalarField).equal("")
      case ScalarFilter(scalarField, In(Vector(NullGCValue)))    => fieldFrom(scalarField).isNull
      case ScalarFilter(scalarField, NotIn(Vector(NullGCValue))) => fieldFrom(scalarField).isNotNull
      case ScalarFilter(scalarField, In(values))                 => fieldFrom(scalarField).in(Vector.fill(values.length) { "" }: _*)
      case ScalarFilter(scalarField, NotIn(values))              => fieldFrom(scalarField).notIn(Vector.fill(values.length) { "" }: _*)
      case OneRelationIsNullFilter(field)                        => oneRelationIsNullFilter(field)
      case x                                                     => sys.error(s"Not supported: $x")
    }
  }
}

object JooqLimitClauseBuilder {

  def limitClause(args: Option[QueryArguments]): String = {
    val (firstOpt, lastOpt, skipOpt) = (args.flatMap(_.first), args.flatMap(_.last), args.flatMap(_.skip))
    validate(args)
    lastOpt.orElse(firstOpt) match {
      case Some(limitedCount) =>
        // Increase by 1 to know if we have a next page / previous page
        s"LIMIT ${limitedCount + 1} OFFSET ${skipOpt.getOrElse(0)}"
      case None =>
        ""
    }
  }

  private def validate(args: Option[QueryArguments]): Unit = {
    throwIfBelowZero(args.flatMap(_.first), InvalidFirstArgument())
    throwIfBelowZero(args.flatMap(_.last), InvalidLastArgument())
    throwIfBelowZero(args.flatMap(_.skip), InvalidSkipArgument())
  }

  private def throwIfBelowZero(opt: Option[Int], exception: Exception): Unit = {
    if (opt.exists(_ < 0)) throw exception
  }
}

object JooqOrderByClauseBuilder {

  def forModel(model: Model, alias: String, args: Option[QueryArguments]): String = {
    internal(
      alias = alias,
      secondaryOrderByField = model.dbNameOfIdField_!,
      args = args
    )
  }

  def forScalarListField(field: ScalarField, alias: String, args: Option[QueryArguments]): String = {
    val (first, last)  = (args.flatMap(_.first), args.flatMap(_.last))
    val isReverseOrder = last.isDefined

    if (first.isDefined && last.isDefined) throw APIErrors.InvalidConnectionArguments()

    // The limit instruction only works from up to down. Therefore, we have to invert order when we use before.
    val order = isReverseOrder match {
      case true  => "desc"
      case false => "asc"
    }

    //always order by nodeId, then positionfield ascending
    s""" ORDER BY "$alias"."nodeId" $order, "$alias"."position" $order """
  }

  def forRelation(relation: Relation, alias: String, args: Option[QueryArguments]): String = {
    internal(
      alias = alias,
      secondaryOrderByField = relation.columnForRelationSide(RelationSide.A),
      args = args
    )
  }

  def internal(alias: String, secondaryOrderByField: String, args: Option[QueryArguments]): String = {
    val (first, last, orderBy) = (args.flatMap(_.first), args.flatMap(_.last), args.flatMap(_.orderBy))
    val isReverseOrder         = last.isDefined

    if (first.isDefined && last.isDefined) throw APIErrors.InvalidConnectionArguments()

    // The limit instruction only works from up to down. Therefore, we have to invert order when we use before.
    val defaultOrder = orderBy.map(_.sortOrder.toString).getOrElse("asc")
    val (order, secondaryOrderByOrder) = isReverseOrder match {
      case true  => (invertOrder(defaultOrder), "desc")
      case false => (defaultOrder, "asc")
    }

    val aliasedSecondaryOrderByField = s""" "$alias"."$secondaryOrderByField" """

    orderBy match {
      case Some(orderByArg) if orderByArg.field.dbName != secondaryOrderByField =>
        val orderByField = s""" "$alias"."${orderByArg.field.dbName}" """

        // First order by the orderByField, then by the secondary order by field to break ties
        s""" ORDER BY $orderByField $order, $aliasedSecondaryOrderByField $secondaryOrderByOrder """

      case _ =>
        s""" ORDER BY $aliasedSecondaryOrderByField $order """
    }
  }

  private def invertOrder(order: String) = order.trim().toLowerCase match {
    case "desc" => "asc"
    case "asc"  => "desc"
    case _      => throw new IllegalArgumentException
  }
}
