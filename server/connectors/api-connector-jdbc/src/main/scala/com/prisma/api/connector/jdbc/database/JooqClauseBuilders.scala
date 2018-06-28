package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector._
import com.prisma.api.schema.APIErrors.{InvalidFirstArgument, InvalidLastArgument, InvalidSkipArgument}
import com.prisma.gc_values.NullGCValue
import com.prisma.shared.models._
import org.jooq.Condition
import org.jooq.impl.DSL._

case class JooqWhereClauseBuilder(slickDatabase: SlickDatabase, schemaName: String) extends BuilderBase {

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
    val idFieldWithAlias = aliasColumn(model.dbNameOfIdField_!)
    val idField          = modelIdColumn(model)

    // First, we fetch the ordering for the query. If none is passed, we order by id, ascending.
    // We need that since before/after are dependent on the order.
    val (orderByField, orderByFieldWithAlias, sortDirection) = orderBy match {
      case Some(order) => (field(name(schemaName, tableName, order.field.dbName)), field(name(topLevelAlias, order.field.dbName)), order.sortOrder.toString)
      case None        => (idField, idFieldWithAlias, "asc")
    }

    val selectQuery = sql
      .select(orderByField)
      .from(table(name(schemaName, tableName)))
      .where(idField.equal(stringDummy))

    // Then, we select the comparison operation and construct the cursors. For instance, if we use ascending order, and we want
    // to get the items before, we use the "<" comparator on the column that defines the order.
    def cursorFor(cursor: String, cursorType: String): Condition = (cursorType, sortDirection.toLowerCase.trim) match {
      case ("before", "asc")  => row(orderByFieldWithAlias, idFieldWithAlias).lessThan(selectQuery, stringDummy)
      case ("before", "desc") => row(orderByFieldWithAlias, idFieldWithAlias).greaterThan(selectQuery, stringDummy)
      case ("after", "asc")   => row(orderByFieldWithAlias, idFieldWithAlias).greaterThan(selectQuery, stringDummy)
      case ("after", "desc")  => row(orderByFieldWithAlias, idFieldWithAlias).lessThan(selectQuery, stringDummy)
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
      case ScalarFilter(scalarField, Contains(_))                => fieldFrom(scalarField).contains(stringDummy)
      case ScalarFilter(scalarField, NotContains(_))             => fieldFrom(scalarField).notContains(stringDummy)
      case ScalarFilter(scalarField, StartsWith(_))              => fieldFrom(scalarField).startsWith(stringDummy)
      case ScalarFilter(scalarField, NotStartsWith(_))           => fieldFrom(scalarField).startsWith(stringDummy).not()
      case ScalarFilter(scalarField, EndsWith(_))                => fieldFrom(scalarField).endsWith(stringDummy)
      case ScalarFilter(scalarField, NotEndsWith(_))             => fieldFrom(scalarField).endsWith(stringDummy).not()
      case ScalarFilter(scalarField, LessThan(_))                => fieldFrom(scalarField).lessThan(stringDummy)
      case ScalarFilter(scalarField, GreaterThan(_))             => fieldFrom(scalarField).greaterThan(stringDummy)
      case ScalarFilter(scalarField, LessThanOrEquals(_))        => fieldFrom(scalarField).lessOrEqual(stringDummy)
      case ScalarFilter(scalarField, GreaterThanOrEquals(_))     => fieldFrom(scalarField).greaterOrEqual(stringDummy)
      case ScalarFilter(scalarField, NotEquals(NullGCValue))     => fieldFrom(scalarField).isNotNull
      case ScalarFilter(scalarField, NotEquals(_))               => fieldFrom(scalarField).notEqual(stringDummy)
      case ScalarFilter(scalarField, Equals(NullGCValue))        => fieldFrom(scalarField).isNull
      case ScalarFilter(scalarField, Equals(x))                  => fieldFrom(scalarField).equal(stringDummy)
      case ScalarFilter(scalarField, In(Vector(NullGCValue)))    => fieldFrom(scalarField).isNull
      case ScalarFilter(scalarField, NotIn(Vector(NullGCValue))) => fieldFrom(scalarField).isNotNull
      case ScalarFilter(scalarField, In(values))                 => fieldFrom(scalarField).in(Vector.fill(values.length) { stringDummy }: _*)
      case ScalarFilter(scalarField, NotIn(values))              => fieldFrom(scalarField).notIn(Vector.fill(values.length) { stringDummy }: _*)
      case OneRelationIsNullFilter(field)                        => oneRelationIsNullFilter(field)
      case x                                                     => sys.error(s"Not supported: $x")
    }
  }
}
