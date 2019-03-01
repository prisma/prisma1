package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.SortOrder.SortOrder
import com.prisma.api.connector._
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models._
import org.jooq
import org.jooq.Condition
import org.jooq.impl.DSL._

trait CursorConditionBuilder extends BuilderBase {
  // This creates a query that checks if the id is in a certain set returned by a subquery Q.
  // The subquery Q fetches all the ID's defined by the cursors and order.
  // On invalid cursor params, no error is thrown. The result set will just be empty.

  def buildCursorCondition(queryArguments: QueryArguments, model: Model): Condition = {
    val (before, after, orderBy) = (queryArguments.before, queryArguments.after, queryArguments.orderBy)
    // If both params are empty, don't generate any query.
    if (before.isEmpty && after.isEmpty) return noCondition()

    val idWithAlias: jooq.Field[AnyRef] = aliasColumn(model.dbNameOfIdField_!)
    val idField: jooq.Field[AnyRef]     = modelIdColumn(model)

    // First, we fetch the ordering for the query. If none is passed, we order by id, ascending.
    // We need that since before/after are dependent on the order.
    val (orderByField: jooq.Field[AnyRef], orderByWithAlias: jooq.Field[AnyRef], sortOrder: SortOrder) = orderBy match {
      case Some(order) => (modelColumn(order.field), aliasColumn(order.field.dbName), order.sortOrder)
      case None        => (idField, idWithAlias, SortOrder.Asc)
    }

    val value: IdGCValue = before match {
      case None     => after.get
      case Some(id) => id
    }

    val cursor = `val`(value.value.asInstanceOf[AnyRef])

    val selectQuery = sql
      .select(orderByField)
      .from(modelTable(model))
      .where(idField.equal(stringDummy))

    // Then, we select the comparison operation and construct the cursors. For instance, if we use ascending order, and we want
    // to get the items before, we use the "<" comparator on the column that defines the order.
    def cursorFor(cursorType: String): Condition = (cursorType, sortOrder) match {
      case ("before", SortOrder.Asc)  => or(and(orderByWithAlias.eq(selectQuery), idWithAlias.lessThan(cursor)), orderByWithAlias.lessThan(selectQuery))
      case ("before", SortOrder.Desc) => or(and(orderByWithAlias.eq(selectQuery), idWithAlias.lessThan(cursor)), orderByWithAlias.greaterThan(selectQuery))
      case ("after", SortOrder.Asc)   => or(and(orderByWithAlias.eq(selectQuery), idWithAlias.greaterThan(cursor)), orderByWithAlias.greaterThan(selectQuery))
      case ("after", SortOrder.Desc)  => or(and(orderByWithAlias.eq(selectQuery), idWithAlias.greaterThan(cursor)), orderByWithAlias.lessThan(selectQuery))
      case _                          => throw new IllegalArgumentException
    }

    val afterCursorFilter: Condition  = after.map(_ => cursorFor("after")).getOrElse(noCondition())
    val beforeCursorFilter: Condition = before.map(_ => cursorFor("before")).getOrElse(noCondition())

    afterCursorFilter.and(beforeCursorFilter)
  }
}
