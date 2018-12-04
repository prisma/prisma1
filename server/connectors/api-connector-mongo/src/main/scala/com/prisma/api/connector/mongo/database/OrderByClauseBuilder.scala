package com.prisma.api.connector.mongo.database

import com.prisma.api.connector.QueryArguments
import com.prisma.api.schema.APIErrors
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.{Document, FindObservable}

object OrderByClauseBuilder {

  def queryWithOrder(query: FindObservable[Document], args: QueryArguments): FindObservable[Document] = {
    val idField                   = "_id"
    val (first, last, orderByArg) = (args.first, args.last, args.orderBy)
    val isReverseOrder            = last.isDefined
    if (first.isDefined && last.isDefined) throw APIErrors.InvalidConnectionArguments()
    // The limit instruction only works from up to down. Therefore, we have to invert order when we use before.
    val defaultOrder = orderByArg.map(_.sortOrder.toString).getOrElse("asc")

    //the secondary field is always ascending no matter what the primary field says. If we need to revert due to last being defined it is always descending.
    (orderByArg, defaultOrder, isReverseOrder) match {
      case (Some(order), "asc", true) if order.field.dbName != idField   => query.sort(orderBy(descending(order.field.dbName), descending(idField)))
      case (Some(order), "desc", true) if order.field.dbName != idField  => query.sort(orderBy(ascending(order.field.dbName), descending(idField)))
      case (Some(order), "asc", false) if order.field.dbName != idField  => query.sort(orderBy(ascending(order.field.dbName), ascending(idField)))
      case (Some(order), "desc", false) if order.field.dbName != idField => query.sort(orderBy(descending(order.field.dbName), ascending(idField)))
      case (_, "asc", true)                                              => query.sort(descending(idField))
      case (_, "desc", true)                                             => query.sort(ascending(idField))
      case (_, "asc", false)                                             => query.sort(ascending(idField))
      case (_, "desc", false)                                            => query.sort(descending(idField))
      case x                                                             => sys.error(s"$x is unhandled in this pattern match")
    }
  }
}
