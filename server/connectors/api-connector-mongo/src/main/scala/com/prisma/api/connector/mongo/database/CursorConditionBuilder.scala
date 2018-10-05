package com.prisma.api.connector.mongo.database

import com.mongodb.client.model.Filters
import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.HackforTrue.hackForTrue
import org.mongodb.scala.bson.conversions

object CursorConditionBuilder {

  def buildCursorCondition(queryArguments: QueryArguments): conversions.Bson = {
    val (before, after, orderBy) = (queryArguments.before, queryArguments.after, queryArguments.orderBy)
    // If both params are empty, don't generate any query.
    if (before.isEmpty && after.isEmpty) return Filters.and(hackForTrue)

    val sortDirection = orderBy match {
      case Some(order) => order.sortOrder.toString
      case None        => "asc"
    }

    def cursorCondition(cursor: String, cursorType: String): conversions.Bson =
      (cursorType, sortDirection.toLowerCase.trim) match {
        case ("before", "asc")  => Filters.lt("_id", cursor)
        case ("before", "desc") => Filters.gt("_id", cursor)
        case ("after", "asc")   => Filters.gt("_id", cursor)
        case ("after", "desc")  => Filters.lt("_id", cursor)
        case _                  => throw new IllegalArgumentException
      }

    val afterCursorCondition  = after.map(cursorCondition(_, "after")).getOrElse(hackForTrue)
    val beforeCursorCondition = before.map(cursorCondition(_, "before")).getOrElse(hackForTrue)

    Filters.and(afterCursorCondition, beforeCursorCondition)
  }

}
