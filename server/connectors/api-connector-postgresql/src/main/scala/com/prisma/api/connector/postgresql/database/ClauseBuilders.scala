package com.prisma.api.connector.postgresql.database

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.LimitClauseBuilder.validate
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.{InvalidFirstArgument, InvalidLastArgument, InvalidSkipArgument}
import com.prisma.gc_values.{GCValue, NullGCValue}
import com.prisma.shared.models._

case class WhereClauseBuilder(schemaName: String) {
  val topLevelAlias: String = QueryBuilders.topLevelAlias

  def buildWhereClause(filter: Option[Filter]): Option[String] = {
    val conditions = buildWhereClauseWithoutWhereKeyWord(filter)
    if (conditions.nonEmpty) Some("WHERE " + conditions) else None
  }

  def buildWhereClauseWithoutWhereKeyWord(filter: Option[Filter]): String = filter match {
    case Some(filter) => buildWheresForFilter(filter, topLevelAlias)
    case None         => "TRUE"
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

  private def buildWheresForFilter(filter: Filter, alias: String): String = {
    def oneRelationIsNullFilter(field: RelationField) = {
      val relation          = field.relation
      val relationTableName = relation.relationTableName
      val column            = relation.columnForRelationSide(field.relationSide)
      val otherIdColumn     = field.relatedModel_!.dbNameOfIdField_!

      s""" not exists (select  *
                from    "$schemaName"."$relationTableName"
                where   "$schemaName"."$relationTableName"."$column" = "$alias"."$otherIdColumn"
              )"""
    }

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
      case ScalarFilter(field, Contains(_))                => column(alias, field) + s""" LIKE ? """
      case ScalarFilter(field, NotContains(_))             => column(alias, field) + s""" NOT LIKE ? """
      case ScalarFilter(field, StartsWith(_))              => column(alias, field) + s""" LIKE ? """
      case ScalarFilter(field, NotStartsWith(_))           => column(alias, field) + s""" NOT LIKE ?"""
      case ScalarFilter(field, EndsWith(_))                => column(alias, field) + s""" LIKE ?"""
      case ScalarFilter(field, NotEndsWith(_))             => column(alias, field) + s""" NOT LIKE ?"""
      case ScalarFilter(field, LessThan(_))                => column(alias, field) ++ s""" < ?"""
      case ScalarFilter(field, GreaterThan(_))             => column(alias, field) ++ s""" > ?"""
      case ScalarFilter(field, LessThanOrEquals(_))        => column(alias, field) ++ s""" <= ?"""
      case ScalarFilter(field, GreaterThanOrEquals(_))     => column(alias, field) ++ s""" >= ?"""
      case ScalarFilter(field, NotEquals(NullGCValue))     => column(alias, field) ++ s""" IS NOT NULL"""
      case ScalarFilter(field, NotEquals(_))               => column(alias, field) ++ s""" != ?"""
      case ScalarFilter(field, Equals(NullGCValue))        => column(alias, field) + s""" IS NULL"""
      case ScalarFilter(field, Equals(_))                  => column(alias, field) + s""" = ?"""
      case ScalarFilter(field, In(Vector(NullGCValue)))    => if (field.isRequired) s"false" else column(alias, field) ++ s""" IS NULL"""
      case ScalarFilter(field, NotIn(Vector(NullGCValue))) => if (field.isRequired) s"true" else column(alias, field) ++ s""" IS NOT NULL"""
      case ScalarFilter(field, In(values))                 => if (values.nonEmpty) column(alias, field) ++ in(values) else s"false"
      case ScalarFilter(field, NotIn(values))              => if (values.nonEmpty) column(alias, field) ++ s""" NOT """ ++ in(values) else s"true"
      case OneRelationIsNullFilter(field)                  => oneRelationIsNullFilter(field)
      case x                                               => sys.error(s"Not supported: $x")
    }
  }

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

  private def column(alias: String, field: Field): String = s""""$alias"."${field.dbName}" """
  private def in(items: Vector[GCValue])                  = s" IN (" + items.map(_ => "?").mkString(",") + ")"
}

object LimitClauseBuilder {
  // Increase by 1 to know if we have a next page / previous page
  def limitClauseForWindowFunction(args: Option[QueryArguments]): String = {
    val (firstOpt, lastOpt, skipOpt) = (args.flatMap(_.first), args.flatMap(_.last), args.flatMap(_.skip))
    validate(args)

    lastOpt.orElse(firstOpt) match {
      case Some(limitedCount) => s" Between ${skipOpt.getOrElse(0)} And ${limitedCount + skipOpt.getOrElse(0) + 1} "
      case None               => " < 99999999999999"
    }
  }

  def limitClause(args: Option[QueryArguments]): String = {
    val (firstOpt, lastOpt, skipOpt) = (args.flatMap(_.first), args.flatMap(_.last), args.flatMap(_.skip))
    validate(args)
    lastOpt.orElse(firstOpt) match {
      case Some(limitedCount) => s"LIMIT ${limitedCount + 1} OFFSET ${skipOpt.getOrElse(0)}"
      case None               => ""
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

object OrderByClauseBuilder {

  def forModel(model: Model, alias: String, args: Option[QueryArguments]): String = {
    internal(
      alias = alias,
      secondaryAlias = alias,
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
      secondaryAlias = alias,
      secondaryOrderByField = relation.columnForRelationSide(RelationSide.A),
      args = args
    )
  }

  def internal(alias: String, secondaryAlias: String, secondaryOrderByField: String, args: Option[QueryArguments]): String = {
    val (first, last, orderBy) = (args.flatMap(_.first), args.flatMap(_.last), args.flatMap(_.orderBy))
    val isReverseOrder         = last.isDefined

    if (first.isDefined && last.isDefined) throw APIErrors.InvalidConnectionArguments()

    // The limit instruction only works from up to down. Therefore, we have to invert order when we use before.
    val defaultOrder = orderBy.map(_.sortOrder.toString).getOrElse("asc")
    val (order, secondaryOrderByOrder) = isReverseOrder match {
      case true  => (invertOrder(defaultOrder), "desc")
      case false => (defaultOrder, "asc")
    }

    val aliasedSecondaryOrderByField = s""" "$secondaryAlias"."$secondaryOrderByField" """

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
