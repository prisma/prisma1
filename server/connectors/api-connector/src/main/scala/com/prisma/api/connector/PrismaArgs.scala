package com.prisma.api.connector

import com.prisma.gc_values.GCValue
import com.prisma.shared.models.{Field, ScalarField}

case class PrismaArgs(raw: GCValue) {
  def hasArgFor(field: ScalarField) = raw.asRoot.map.get(field.name).isDefined

  def getFieldValue(name: String): Option[GCValue] = raw.asRoot.map.get(name)

  def keys: Vector[String] = raw.asRoot.map.keys.toVector
}
