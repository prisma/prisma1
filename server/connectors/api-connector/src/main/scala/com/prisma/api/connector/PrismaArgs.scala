package com.prisma.api.connector

import com.prisma.gc_values.{DateTimeGCValue, GCValue, RootGCValue}
import com.prisma.shared.models.{Model, ScalarField}
import org.joda.time.{DateTime, DateTimeZone}

case class PrismaArgs(raw: GCValue) {
  lazy val rootGC                                        = raw.asRoot
  lazy val rootGCMap: Map[String, GCValue]               = rootGC.map
  lazy val isEmpty                                       = rootGCMap.isEmpty
  def hasArgFor(field: ScalarField): Boolean             = rootGCMap.get(field.name).isDefined
  def getFieldValue(field: ScalarField): Option[GCValue] = rootGCMap.get(field.name)
  def getFieldValue(name: String): Option[GCValue]       = rootGCMap.get(name)

  def keys: Vector[String] = raw.asRoot.map.keys.toVector

  def addDateTimesIfNecessary(model: Model) =
    if (isEmpty) {
      this
    } else {
      lazy val now = DateTimeGCValue(DateTime.now(DateTimeZone.UTC))
      (model.hasCreatedAtField, model.hasUpdatedAtField) match {
        case (true, true)   => PrismaArgs(RootGCValue(rootGCMap + ("createdAt" -> now) + ("updatedAt" -> now)))
        case (true, false)  => PrismaArgs(RootGCValue(rootGCMap + ("createdAt" -> now)))
        case (false, true)  => PrismaArgs(RootGCValue(rootGCMap + ("updatedAt" -> now)))
        case (false, false) => this
      }
    }

  def updateDateTimesIfNecessary(model: Model) =
    if (isEmpty) {
      this
    } else {
      lazy val now = DateTimeGCValue(DateTime.now(DateTimeZone.UTC))
      model.hasUpdatedAtField match {
        case true  => PrismaArgs(RootGCValue(rootGCMap + ("updatedAt" -> now)))
        case false => this
      }
    }
}
