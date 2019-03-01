package com.prisma.api.connector.mongo.database

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters._
import com.prisma.api.connector.SortOrder.SortOrder
import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.DocumentToRoot
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.gc_values.{GCValue, IdGCValue, StringIdGCValue}
import com.prisma.shared.models.Model
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.bson.conversions.Bson

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
object CursorConditionBuilder {

  def fetchCursorRowValueById(database: MongoDatabase, model: Model, queryArguments: QueryArguments): Future[Option[GCValue]] = {
    val (before, after, orderBy) = (queryArguments.before, queryArguments.after, queryArguments.orderBy)
    // If both params are empty, don't generate any query.
    if (before.isEmpty && after.isEmpty) return Future.successful(None)

    val orderByDBName = orderBy.map(_.field.dbName).getOrElse("_id")
    val orderByName   = orderBy.map(_.field.name).getOrElse(model.idField_!.name)

    val value: IdGCValue = before match {
      case None     => after.get
      case Some(id) => id
    }

    val cursor = GCToBson(value.asInstanceOf[StringIdGCValue])
    import org.mongodb.scala.model.Projections._
    for {
      res            <- database.getCollection(model.dbName).find(Filters.eq("_id", cursor)).projection(include(orderByDBName)).collect().toFuture()
      docOption      = res.headOption
      rootOption     = docOption.map(doc => DocumentToRoot(model, doc))
      rowValueOption = rootOption.map(root => root.map(orderByName))
    } yield rowValueOption
  }

  def buildCursorCondition(model: Model, queryArguments: QueryArguments, rowValue: GCValue): conversions.Bson = {
    val (before, after, orderBy) = (queryArguments.before, queryArguments.after, queryArguments.orderBy)
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
      case (Some(after), Some(before)) => Filters.and(after, before)
      case (Some(after), None)         => after
      case (None, Some(before))        => before
      case (None, None)                => sys.error("One of either Before or After should be defined here.")
    }
  }
}
