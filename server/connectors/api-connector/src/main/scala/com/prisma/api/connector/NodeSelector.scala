package com.prisma.api.connector

import com.prisma.gc_values.{CuidGCValue, GCValue, IdGcValue}
import com.prisma.shared.models.{Model, ScalarField}

object NodeSelector {
  def forCuid(model: Model, id: String): NodeSelector              = NodeSelector(model, model.idField_!, CuidGCValue(id))
  def forIdGCValue(model: Model, gcValue: GCValue): NodeSelector   = NodeSelector(model, model.idField_!, gcValue)
  def forGCValue(model: Model, field: ScalarField, value: GCValue) = NodeSelector(model, field, value)
}

case class NodeSelector(model: Model, field: ScalarField, fieldGCValue: GCValue) {
  lazy val value         = fieldGCValue.value
  lazy val fieldName     = field.name
  lazy val isId: Boolean = field.name == "id"
}
