package com.prisma.api.connector.mongo.database

import com.mongodb.client.model.Filters
import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.gc_values.StringIdGCValue
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.bson.{ObjectId, conversions}

object CursorConditionBuilder {

  def buildCursorCondition(queryArguments: QueryArguments): Option[conversions.Bson] = {
    val (before, after, orderBy) = (queryArguments.before, queryArguments.after, queryArguments.orderBy)
    // If both params are empty, don't generate any query.
    if (before.isEmpty && after.isEmpty) return None

    val sortDirection = orderBy match {
      case Some(order) => order.sortOrder.toString
      case None        => "asc"
    }

    def cursorCondition(cursor: String, cursorType: String): conversions.Bson = {
      val objectId = GCToBson(StringIdGCValue(cursor))

      (cursorType, sortDirection.toLowerCase.trim) match {
        case ("before", "asc")  => Filters.lt("_id", objectId)
        case ("before", "desc") => Filters.gt("_id", objectId)
        case ("after", "asc")   => Filters.gt("_id", objectId)
        case ("after", "desc")  => Filters.lt("_id", objectId)
        case _                  => throw new IllegalArgumentException
      }
    }

    val afterCursorCondition: Option[Bson]  = after.map(_.asInstanceOf[StringIdGCValue].value).map(cursorCondition(_, "after"))
    val beforeCursorCondition: Option[Bson] = before.map(_.asInstanceOf[StringIdGCValue].value).map(cursorCondition(_, "before"))

    (afterCursorCondition, beforeCursorCondition) match {
      case (Some(after), Some(before)) => Some(Filters.and(after, before))
      case (Some(after), None)         => Some(after)
      case (None, Some(before))        => Some(before)
      case (None, None)                => None
    }
  }

}
