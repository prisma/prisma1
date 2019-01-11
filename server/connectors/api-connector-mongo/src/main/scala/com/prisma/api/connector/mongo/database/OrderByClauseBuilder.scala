package com.prisma.api.connector.mongo.database

import com.prisma.api.connector.QueryArguments
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.ReservedFields
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Sorts._

object OrderByClauseBuilder {

  def sortStage(args: QueryArguments): conversions.Bson = sort(sortBson(args))
  def sortBson(args: QueryArguments): conversions.Bson = {

    val idField                   = ReservedFields.mongoInternalIdfieldName
    val (first, last, orderByArg) = (args.first, args.last, args.orderBy)
    val isReverseOrder            = last.isDefined
    if (first.isDefined && last.isDefined) throw APIErrors.InvalidConnectionArguments()
    // The limit instruction only works from up to down. Therefore, we have to invert order when we use before.
    val defaultOrder = orderByArg.map(_.sortOrder.toString).getOrElse("asc")

    //the secondary field is always ascending no matter what the primary field says. If we need to revert due to last being defined it is always descending.
    (orderByArg, defaultOrder, isReverseOrder) match {
      case (Some(order), "asc", true) if order.field.dbName != idField   => orderBy(descending(order.field.dbName), descending(idField))
      case (Some(order), "desc", true) if order.field.dbName != idField  => orderBy(ascending(order.field.dbName), descending(idField))
      case (Some(order), "asc", false) if order.field.dbName != idField  => orderBy(ascending(order.field.dbName), ascending(idField))
      case (Some(order), "desc", false) if order.field.dbName != idField => orderBy(descending(order.field.dbName), ascending(idField))
      case (_, "asc", true)                                              => descending(idField)
      case (_, "desc", true)                                             => ascending(idField)
      case (_, "asc", false)                                             => ascending(idField)
      case (_, "desc", false)                                            => descending(idField)
      case x                                                             => sys.error(s"$x is unhandled in this pattern match")
    }
  }
}
