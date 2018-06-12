package com.prisma.api.connector.postgresql.database

import java.sql.Connection

import com.prisma.api.connector._
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.{InvalidFirstArgument, InvalidLastArgument, InvalidSkipArgument}
import com.prisma.gc_values.{GCValue, NullGCValue}
import com.prisma.shared.models._
import org.jooq.{Condition, SQLDialect}
import org.jooq._
import org.jooq.impl._
import org.jooq.impl.DSL._
import collection.JavaConverters._
import org.jooq.scalaextensions.Conversions._

case class JooqWhereClauseBuilder(connection: Connection, schemaName: String) {
  val topLevelAlias: String = QueryBuilders.topLevelAlias
  val sql                   = DSL.using(connection, SQLDialect.POSTGRES_9_5)

  def buildWhereClause(filter: Option[Filter]): Vector[Condition] = filter match {
    case Some(filter) => buildWheresForFilter(filter, topLevelAlias)
    case None         => Vector.empty
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

  private def buildWheresForFilter(filter: Filter, alias: String): Vector[Condition] = {
    def oneRelationIsNullFilter(field2: RelationField): Condition = {
      val relation          = field2.relation
      val relationTableName = relation.relationTableName
      val column            = relation.columnForRelationSide(field2.relationSide)
      val otherIdColumn     = field2.relatedModel_!.dbNameOfIdField_!

      val select = sql
        .select()
        .from(name(schemaName, relationTableName))
        .where(field(name(schemaName, relationTableName, column)).eq(field(name(alias, otherIdColumn))))

      trueCondition().andNotExists(select)
    }

    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter()                       => Vector.empty
      case AndFilter(filters)                             => filters.flatMap(buildWheresForFilter(_, alias))
      case OrFilter(filters)                              => Vector(trueCondition())
      case NotFilter(filters)                             => Vector(trueCondition())
      case NodeFilter(filters)                            => Vector(trueCondition())
      case RelationFilter(field, nestedFilter, condition) => Vector(trueCondition())
      //--------------------------------ANCHORS------------------------------------
      case PreComputedSubscriptionFilter(value)            => if (value) Vector(trueCondition()) else Vector(falseCondition())
      case ScalarFilter(field, Contains(_))                => Vector(trueCondition())
      case ScalarFilter(field, NotContains(_))             => Vector(trueCondition())
      case ScalarFilter(field, StartsWith(_))              => Vector(trueCondition())
      case ScalarFilter(field, NotStartsWith(_))           => Vector(trueCondition())
      case ScalarFilter(field, EndsWith(_))                => Vector(trueCondition())
      case ScalarFilter(field, NotEndsWith(_))             => Vector(trueCondition())
      case ScalarFilter(field, LessThan(_))                => Vector(trueCondition())
      case ScalarFilter(field, GreaterThan(_))             => Vector(trueCondition())
      case ScalarFilter(field, LessThanOrEquals(_))        => Vector(trueCondition())
      case ScalarFilter(field, GreaterThanOrEquals(_))     => Vector(trueCondition())
      case ScalarFilter(field, NotEquals(NullGCValue))     => Vector(trueCondition())
      case ScalarFilter(field, NotEquals(_))               => Vector(trueCondition())
      case ScalarFilter(field, Equals(NullGCValue))        => Vector(trueCondition())
      case ScalarFilter(field2, Equals(x))                 => Vector(field(name(alias, field2.dbName)) === x.value)
      case ScalarFilter(field, In(Vector(NullGCValue)))    => Vector(trueCondition())
      case ScalarFilter(field, NotIn(Vector(NullGCValue))) => Vector(trueCondition())
      case ScalarFilter(field, In(values))                 => Vector(trueCondition())
      case ScalarFilter(field, NotIn(values))              => Vector(trueCondition())
      case OneRelationIsNullFilter(field)                  => Vector(oneRelationIsNullFilter(field))
      case x                                               => sys.error(s"Not supported: $x")
    }
  }
//
//  private def equalsGcValue(scalarField: ScalarField) = scalarField.typeIdentifier match {
//    case TypeIdentifier.Int => field(scalarField.dbName, classOf[Long]).eq(12)
//      Vector(field(name(alias, field2.dbName), classOf[Long]).eq(x.value.asInstanceOf[Long]))
//  }

  private def relationFilterStatement(alias: String, field: RelationField, nestedFilter: Filter, relationCondition: RelationCondition): String = {
    val relationTableName = field.relation.relationTableName
    val column            = field.relation.columnForRelationSide(field.relationSide)
    val oppositeColumn    = field.relation.columnForRelationSide(field.oppositeRelationSide)

    val newAlias = field.relatedModel_!.dbName + "_" + alias

    val join = s"""select *
            from "$schemaName"."${field.relatedModel_!.dbName}" as "$newAlias"
            inner join "$schemaName"."$relationTableName"
            on "$newAlias"."${field.relatedModel_!.dbNameOfIdField_!}" = "$schemaName"."$relationTableName"."$oppositeColumn"
            where "$schemaName"."$relationTableName"."$column" = "$alias"."${field.model.dbNameOfIdField_!}" """

    val nestedFilterStatement = {
      val x = buildWheresForFilter(nestedFilter, newAlias)
      if (x.isEmpty) "TRUE" else x
    }

    relationCondition match {
      case AtLeastOneRelatedNode => s" exists (" + join + s"and " + nestedFilterStatement + ")"
      case EveryRelatedNode      => s" not exists (" + join + s"and not " + nestedFilterStatement + ")"
      case NoRelatedNode         => s" not exists (" + join + s"and " + nestedFilterStatement + ")"
      case NoRelationCondition   => s" exists (" + join + s"and " + nestedFilterStatement + ")"
    }
  }

  //  private def column(alias: String, field: Field): String = s""""$alias"."${field.dbName}" """
  private def in(items: Vector[GCValue]) = s" IN (" + items.map(_ => "?").mkString(",") + ")"
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
