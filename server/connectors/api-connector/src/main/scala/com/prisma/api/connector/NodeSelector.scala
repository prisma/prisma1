package com.prisma.api.connector

import com.prisma.gc_values.{GCValue, GCValueExtractor, IdGCValue}
import com.prisma.shared.models.{Field, Model}

object NodeSelector {
  def forId(model: Model, id: String): NodeSelector                  = NodeSelector(model, model.getFieldByName_!("id"), IdGCValue(id))
  def forIdGCValue(model: Model, idGCValue: IdGCValue): NodeSelector = NodeSelector(model, model.getFieldByName_!("id"), idGCValue)
  def forGCValue(model: Model, field: Field, value: GCValue)         = NodeSelector(model, field, value)
}

case class NodeSelector(model: Model, field: Field, fieldValue: GCValue) {
  lazy val fieldName                  = field.name
  lazy val fieldValueAsString: String = GCValueExtractor.fromGCValueToString(fieldValue)
  lazy val isId: Boolean              = field.name == "id"
}
