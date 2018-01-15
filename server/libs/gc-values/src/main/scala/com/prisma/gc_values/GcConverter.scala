package com.prisma.gc_values

import org.scalactic.Or

trait GCConverter[T] extends FromGcValue[T] with ToGcValue[T]

trait ToGcValue[T] {
  def toGCValue(t: T): Or[GCValue, InvalidValueForScalarType]
}

trait FromGcValue[T] {
  def fromGCValue(gcValue: GCValue): T
}

case class InvalidValueForScalarType(value: String, typeIdentifier: String)
