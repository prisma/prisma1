package com.prisma.api.connector

import com.prisma.gc_values.{GCValue, GCValueExtractor, IdGCValue}
import com.prisma.shared.models.{Field, Model, ScalarField}

object NodeSelector {
  def forId(model: Model, id: String): NodeSelector                = NodeSelector(model, model.getScalarFieldByName_!("id"), IdGCValue(id))
  def forIdGCValue(model: Model, gcValue: GCValue): NodeSelector   = NodeSelector(model, model.getScalarFieldByName_!("id"), gcValue)
  def forGCValue(model: Model, field: ScalarField, value: GCValue) = NodeSelector(model, field, value)
}

case class NodeSelector(model: Model, field: ScalarField, fieldValue: GCValue) {
  lazy val fieldName                  = field.name
  lazy val fieldValueAsString: String = GCValueExtractor.fromGCValueToString(fieldValue)
  lazy val isId: Boolean              = field.name == "id"
}
