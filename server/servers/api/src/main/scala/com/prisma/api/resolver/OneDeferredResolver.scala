package com.prisma.api.resolver

import com.prisma.api.connector.{DataResolver, PrismaNode}
import com.prisma.api.resolver.DeferredTypes._
import com.prisma.shared.models.Project
import com.prisma.util.gc_value.GCAnyConverter

import scala.concurrent.ExecutionContext.Implicits.global

class OneDeferredResolver(dataResolver: DataResolver) {
  def resolve(orderedDeferreds: Vector[OrderedDeferred[OneDeferred]]): Vector[OrderedDeferredFutureResult[OneDeferredResultType]] = {
    val deferreds = orderedDeferreds.map(_.deferred)

    // check if we really can satisfy all deferreds with one database query
    DeferredUtils.checkSimilarityOfOneDeferredsAndThrow(deferreds)

    val headDeferred = deferreds.head
    val converter    = GCAnyConverter(headDeferred.model.getFieldByName_!(headDeferred.key).typeIdentifier, false)
    val deferredsWithGCValues = deferreds.map { deferred =>
      val convertedValue = converter.toGCValue(deferred.value).get
      OneDeferredGC(deferred.model, deferred.key, convertedValue)
    }

    // fetch prismaNodes
    val futurePrismaNodes = dataResolver.batchResolveByUnique(headDeferred.model, headDeferred.key, deferredsWithGCValues.map(_.value))

    // assign the dataitem that was requested by each deferred
    orderedDeferreds.map {
      case OrderedDeferred(deferred, order) =>
        OrderedDeferredFutureResult[OneDeferredResultType](futurePrismaNodes.map { vector =>
          dataItemsToToOneDeferredResultType(dataResolver.project, deferred, vector).map(_.toDataItem)
        }, order)
    }
  }

  private def dataItemsToToOneDeferredResultType(project: Project, deferred: OneDeferred, prismaNodes: Vector[PrismaNode]): Option[PrismaNode] = {
    deferred.key match {
      case "id" => prismaNodes.find(_.id == deferred.value)
      case _ =>
        prismaNodes.find { dataItem =>
          val itemValue       = dataItem.data.map(deferred.key)
          val field           = deferred.model.getFieldByName_!(deferred.key)
          val whereFieldValue = GCAnyConverter(field.typeIdentifier, field.isList).toGCValue(deferred.value).get
          whereFieldValue == itemValue
        }
    }
  }
}
