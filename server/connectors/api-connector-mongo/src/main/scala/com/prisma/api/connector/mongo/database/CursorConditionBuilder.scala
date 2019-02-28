package com.prisma.api.connector.mongo.database

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters._
import com.prisma.api.connector.SortOrder.SortOrder
import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.DocumentToRoot
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.gc_values.{GCValue, IdGCValue, StringIdGCValue}
import com.prisma.shared.models.Model
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.{Document, MongoDatabase}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
object CursorConditionBuilder {

  def fetchCursorRowValueById(database: MongoDatabase, model: Model, queryArguments: QueryArguments): Future[Option[GCValue]] = {
    val (before, after, orderBy) = (queryArguments.before, queryArguments.after, queryArguments.orderBy)
    // If both params are empty, don't generate any query.
    if (before.isEmpty && after.isEmpty) return Future.successful(None)

    val orderByField = orderBy.map(_.field.dbName).getOrElse("_id")

    val value: IdGCValue = before match {
      case None     => after.get
      case Some(id) => id
    }

    val cursor = GCToBson(value.asInstanceOf[StringIdGCValue])
    import org.mongodb.scala.model.Projections._
    val res: Future[Option[Document]] =
      database.getCollection(model.dbName).find(Filters.eq("_id", cursor)).projection(include(orderByField)).limit(1).collect().toFuture().map(_.headOption)
    res.map(x => x.map(y => DocumentToRoot(model, y).map(orderBy.map(_.field.name).getOrElse(model.idField_!.name))))

  }

  def buildCursorCondition(model: Model, queryArguments: QueryArguments, rowValue: GCValue): Option[conversions.Bson] = {
    val (before, after, orderBy) = (queryArguments.before, queryArguments.after, queryArguments.orderBy)
    // If both params are empty, don't generate any query.
    if (before.isEmpty && after.isEmpty) return None

    val (orderByField: String, sortOrder: SortOrder) = orderBy match {
      case Some(order) => (order.field.dbName, order.sortOrder)
      case None        => ("_id", SortOrder.Asc)
    }

    val value: IdGCValue = before match {
      case None     => after.get
      case Some(id) => id
    }

    val rowVal = GCToBson(rowValue)

    val cursor = GCToBson(value.asInstanceOf[StringIdGCValue])

    def cursorCondition(cursorType: String): conversions.Bson = (cursorType, sortOrder) match {
      case ("before", SortOrder.Asc)  => or(and(Filters.eq(orderByField, rowVal), lt("_id", cursor)), lt(orderByField, rowVal))
      case ("before", SortOrder.Desc) => or(and(Filters.eq(orderByField, rowVal), lt("_id", cursor)), gt(orderByField, rowVal))
      case ("after", SortOrder.Asc)   => or(and(Filters.eq(orderByField, rowVal), gt("_id", cursor)), gt(orderByField, rowVal))
      case ("after", SortOrder.Desc)  => or(and(Filters.eq(orderByField, rowVal), gt("_id", cursor)), lt(orderByField, rowVal))
      case _                          => throw new IllegalArgumentException

    }

    val afterCursorCondition: Option[Bson]  = after.map(_ => cursorCondition("after"))
    val beforeCursorCondition: Option[Bson] = before.map(_ => cursorCondition("before"))

    (afterCursorCondition, beforeCursorCondition) match {
      case (Some(after), Some(before)) => Some(Filters.and(after, before))
      case (Some(after), None)         => Some(after)
      case (None, Some(before))        => Some(before)
      case (None, None)                => None
    }
  }
}
