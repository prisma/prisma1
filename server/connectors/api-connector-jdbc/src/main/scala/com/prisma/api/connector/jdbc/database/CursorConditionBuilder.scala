package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector._
import com.prisma.shared.models._
import org.jooq.Condition
import org.jooq.impl.DSL._

trait CursorConditionBuilder extends BuilderBase {
  // This creates a query that checks if the id is in a certain set returned by a subquery Q.
  // The subquery Q fetches all the ID's defined by the cursors and order.
  // On invalid cursor params, no error is thrown. The result set will just be empty.

  def buildCursorCondition(queryArguments: Option[QueryArguments], model: Model): Condition = queryArguments match {
    case Some(args) => buildCursorCondition(args, model)
    case None       => trueCondition()
  }

  private def buildCursorCondition(queryArguments: QueryArguments, model: Model): Condition = {
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

}
