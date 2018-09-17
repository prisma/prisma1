package com.prisma.api.connector.mongo.database

import com.mongodb.client.model.Filters
import com.prisma.api.connector._
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Filters.notEqual

object HackforTrue {
  val hackForTrue = notEqual("_id", -1)
}

object CursorConditionBuilder {

  def buildCursorCondition(queryArguments: Option[QueryArguments]): conversions.Bson = queryArguments match {
    case Some(args) => buildCursorCondition(args)
    case None       => Filters.and(HackforTrue.hackForTrue)
  }

  private def buildCursorCondition(queryArguments: QueryArguments): conversions.Bson = {
    val (before, after, orderBy) = (queryArguments.before, queryArguments.after, queryArguments.orderBy)
    // If both params are empty, don't generate any query.
    if (before.isEmpty && after.isEmpty) return Filters.and(HackforTrue.hackForTrue)

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

    val afterCursorCondition  = after.map(cursorCondition(_, "after")).getOrElse(HackforTrue.hackForTrue)
    val beforeCursorCondition = before.map(cursorCondition(_, "before")).getOrElse(HackforTrue.hackForTrue)

    Filters.and(afterCursorCondition, beforeCursorCondition)
  }

}
