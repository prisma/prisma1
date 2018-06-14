package com.prisma.api.connector.postgresql.database

import com.prisma.api.connector._
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.{InvalidFirstArgument, InvalidLastArgument, InvalidSkipArgument}
import com.prisma.gc_values.NullGCValue
import com.prisma.shared.models._
import org.jooq.conf.Settings
import org.jooq.impl.DSL._
import org.jooq.impl._
import org.jooq.{Condition, SQLDialect, SortField}

case class JooqWhereClauseBuilder(schemaName: String) {
  val topLevelAlias: String = QueryBuilders.topLevelAlias
  val sql                   = DSL.using(SQLDialect.POSTGRES_9_5, new Settings().withRenderFormatted(true))

  def buildWhereClause(filter: Option[Filter]): Option[Condition] = filter match {
    case Some(filter) => Some(buildWheresForFilter(filter, topLevelAlias))
    case None         => None
  }

  // This creates a query that checks if the id is in a certain set returned by a subquery Q.
  // The subquery Q fetches all the ID's defined by the cursors and order.
  // On invalid cursor params, no error is thrown. The result set will just be empty.

  def buildCursorCondition(queryArguments: Option[QueryArguments], model: Model): Condition = queryArguments match {
    case Some(args) => buildCursorCondition(args, model)
    case None       => trueCondition()
  }

  def buildCursorCondition(queryArguments: QueryArguments, model: Model): Condition = {
    val (before, after, orderBy) = (queryArguments.before, queryArguments.after, queryArguments.orderBy)
    // If both params are empty, don't generate any query.
    if (before.isEmpty && after.isEmpty) return trueCondition()

    val tableName        = model.dbName
    val idFieldWithAlias = field(name(topLevelAlias, model.dbNameOfIdField_!))
    val idField          = field(name(schemaName, tableName, model.dbNameOfIdField_!))

    // First, we fetch the ordering for the query. If none is passed, we order by id, ascending.
    // We need that since before/after are dependent on the order.
    val (orderByField, orderByFieldWithAlias, sortDirection) = orderBy match {
      case Some(order) => (field(name(schemaName, tableName, order.field.dbName)), field(name(topLevelAlias, order.field.dbName)), order.sortOrder.toString)
      case None        => (idField, idFieldWithAlias, "asc")
    }

    val selectQuery = sql
      .select(orderByField)
      .from(table(name(schemaName, tableName)))
      .where(idField.equal(""))

    // Then, we select the comparison operation and construct the cursors. For instance, if we use ascending order, and we want
    // to get the items before, we use the "<" comparator on the column that defines the order.
    def cursorFor(cursor: String, cursorType: String): Condition = (cursorType, sortDirection.toLowerCase.trim) match {
      case ("before", "asc")  => row(orderByFieldWithAlias, idFieldWithAlias).lessThan(selectQuery, "")
      case ("before", "desc") => row(orderByFieldWithAlias, idFieldWithAlias).greaterThan(selectQuery, "")
      case ("after", "asc")   => row(orderByFieldWithAlias, idFieldWithAlias).greaterThan(selectQuery, "")
      case ("after", "desc")  => row(orderByFieldWithAlias, idFieldWithAlias).lessThan(selectQuery, "")
      case _                  => throw new IllegalArgumentException
    }

    val afterCursorFilter  = after.map(cursorFor(_, "after")).getOrElse(trueCondition())
    val beforeCursorFilter = before.map(cursorFor(_, "before")).getOrElse(trueCondition())

    afterCursorFilter.and(beforeCursorFilter)
  }

  private def buildWheresForFilter(filter: Filter, alias: String): Condition = {
    def oneRelationIsNullFilter(relationField: RelationField): Condition = {
      val relation          = relationField.relation
      val relationTableName = relation.relationTableName
      val column            = relation.columnForRelationSide(relationField.relationSide)
      val otherIdColumn     = relationField.relatedModel_!.dbNameOfIdField_!

      val select = sql
        .select()
        .from(table(name(schemaName, relationTableName)))
        .where(field(name(schemaName, relationTableName, column)).eq(field(name(alias, otherIdColumn))))

      notExists(select)
    }

    def relationFilterStatement(alias: String, relationFilter: RelationFilter): Condition = {
      val relationField         = relationFilter.field
      val relationTableName     = relationField.relation.relationTableName
      val column                = relationField.relation.columnForRelationSide(relationField.relationSide)
      val oppositeColumn        = relationField.relation.columnForRelationSide(relationField.oppositeRelationSide)
      val newAlias              = relationField.relatedModel_!.dbName + "_" + alias
      val nestedFilterStatement = buildWheresForFilter(relationFilter.nestedFilter, newAlias)

      val select = sql
        .select()
        .from(table(name(schemaName, relationField.relatedModel_!.dbName)).as(newAlias))
        .innerJoin(table(name(schemaName, relationTableName)))
        .on(field(name(newAlias, relationField.relatedModel_!.dbNameOfIdField_!)).eq(field(name(schemaName, relationTableName, oppositeColumn))))
        .where(field(name(schemaName, relationTableName, column)).eq(field(name(alias, relationField.model.dbNameOfIdField_!))))

      relationFilter.condition match {
        case AtLeastOneRelatedNode => exists(select.and(nestedFilterStatement))
        case EveryRelatedNode      => notExists(select.andNot(nestedFilterStatement))
        case NoRelatedNode         => notExists(select.and(nestedFilterStatement))
        case NoRelationCondition   => exists(select.and(nestedFilterStatement))
      }
    }

    def fieldFrom(scalarField: ScalarField) = field(name(alias, scalarField.dbName))
    def nonEmptyConditions(filters: Vector[Filter]) = filters.map(buildWheresForFilter(_, alias)) match {
      case x if x.isEmpty => Vector(trueCondition())
      case x              => x
    }

    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter() => and(trueCondition())
      case AndFilter(filters)       => nonEmptyConditions(filters).reduceLeft(_ and _)
      case OrFilter(filters)        => nonEmptyConditions(filters).reduceLeft(_ or _)
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

  def limitClause(args: Option[QueryArguments]): Option[(Int, Int)] = {
    val (firstOpt, lastOpt, skipOpt) = (args.flatMap(_.first), args.flatMap(_.last), args.flatMap(_.skip))
    validate(args)
    lastOpt.orElse(firstOpt) match {
      case Some(limitedCount) => Some(limitedCount + 1, skipOpt.getOrElse(0))
      case None               => None
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
  import org.jooq.impl.DSL._

  def forModel(model: Model, alias: String, args: Option[QueryArguments]): Vector[SortField[AnyRef]] = {
    internal(
      alias = alias,
      secondaryAlias = alias,
      secondOrderField = model.dbNameOfIdField_!,
      args = args
    )
  }

  def forScalarListField(alias: String, args: Option[QueryArguments]): Vector[SortField[AnyRef]] = {
    val (first, last)  = (args.flatMap(_.first), args.flatMap(_.last))
    val isReverseOrder = last.isDefined

    if (first.isDefined && last.isDefined) throw APIErrors.InvalidConnectionArguments()

    val nodeIdField   = field(name(alias, "nodeId"))
    val positionField = field(name(alias, "position"))

    // The limit instruction only works from up to down. Therefore, we have to invert order when we use before.
    isReverseOrder match {
      case true  => Vector(nodeIdField.desc, positionField.desc)
      case false => Vector(nodeIdField.asc, positionField.asc)
    }
  }

  def forRelation(relation: Relation, alias: String, args: Option[QueryArguments]): Vector[SortField[AnyRef]] = {
    internal(
      alias = alias,
      secondaryAlias = alias,
      secondOrderField = relation.columnForRelationSide(RelationSide.A),
      args = args
    )
  }

  def internal(alias: String, secondaryAlias: String, secondOrderField: String, args: Option[QueryArguments]): Vector[SortField[AnyRef]] = {
    val (first, last, orderBy) = (args.flatMap(_.first), args.flatMap(_.last), args.flatMap(_.orderBy))
    val isReverseOrder         = last.isDefined
    if (first.isDefined && last.isDefined) throw APIErrors.InvalidConnectionArguments()
    // The limit instruction only works from up to down. Therefore, we have to invert order when we use before.
    val defaultOrder = orderBy.map(_.sortOrder.toString).getOrElse("asc")
    val secondField  = field(name(secondaryAlias, secondOrderField))

    (orderBy, defaultOrder, isReverseOrder) match {
      case (Some(order), "asc", true) if order.field.dbName != secondOrderField   => Vector(field(name(alias, order.field.dbName)).desc(), secondField.desc())
      case (Some(order), "desc", true) if order.field.dbName != secondOrderField  => Vector(field(name(alias, order.field.dbName)).asc(), secondField.asc())
      case (Some(order), "asc", false) if order.field.dbName != secondOrderField  => Vector(field(name(alias, order.field.dbName)).asc(), secondField.asc())
      case (Some(order), "desc", false) if order.field.dbName != secondOrderField => Vector(field(name(alias, order.field.dbName)).desc(), secondField.desc())
      case (_, "asc", true)                                                       => Vector(secondField.desc())
      case (_, "desc", true)                                                      => Vector(secondField.asc())
      case (_, "asc", false)                                                      => Vector(secondField.asc())
      case (_, "desc", false)                                                     => Vector(secondField.desc())
      case _                                                                      => throw new IllegalArgumentException
    }
  }
}
