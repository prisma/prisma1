package com.prisma.api.connector

import com.prisma.gc_values.{DateTimeGCValue, GCValue, RootGCValue}
import com.prisma.shared.models.{Model, ScalarField}
import org.joda.time.{DateTime, DateTimeZone}

case class PrismaArgs(raw: GCValue) {
  def hasArgFor(field: ScalarField)                      = raw.asRoot.map.get(field.name).isDefined
  def getFieldValue(field: ScalarField): Option[GCValue] = raw.asRoot.map.get(field.name)
  def getFieldValue(name: String): Option[GCValue]       = raw.asRoot.map.get(name)

  def keys: Vector[String] = raw.asRoot.map.keys.toVector

  def addDateTimesIfNecessary(model: Model) = {
    lazy val now = DateTimeGCValue(DateTime.now(DateTimeZone.UTC))
    (model.hasCreatedAtField, model.hasUpdatedAtField) match {
      case (true, true)   => PrismaArgs(RootGCValue(raw.asRoot.map + ("createdAt" -> now) + ("updatedAt" -> now)))
      case (true, false)  => PrismaArgs(RootGCValue(raw.asRoot.map + ("createdAt" -> now)))
      case (false, true)  => PrismaArgs(RootGCValue(raw.asRoot.map + ("updatedAt" -> now)))
      case (false, false) => this
    }
  }

  def updateDateTimesIfNecessary(model: Model) = {
    lazy val now = DateTimeGCValue(DateTime.now(DateTimeZone.UTC))
    model.hasUpdatedAtField match {
      case true  => PrismaArgs(RootGCValue(raw.asRoot.map + ("updatedAt" -> now)))
      case false => this
    }
  }
}
