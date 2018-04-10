package com.prisma.api.connector

import com.prisma.gc_values.{GCValue, IdGCValue}
import com.prisma.shared.models.{Field, Model}
import com.prisma.util.gc_value.{GCAnyConverter, GCValueExtractor}

object NodeSelector {
  def forId(model: Model, id: String): NodeSelector                  = NodeSelector(model, model.getFieldByName_!("id"), IdGCValue(id))
  def forIdGCValue(model: Model, idGCValue: IdGCValue): NodeSelector = NodeSelector(model, model.getFieldByName_!("id"), idGCValue)
  def forGCValue(model: Model, field: Field, value: GCValue)         = NodeSelector(model, field, value)
}

case class NodeSelector(model: Model, field: Field, fieldValue: GCValue) {
  lazy val fieldName                  = field.name
  lazy val fieldValueAsString: String = GCValueExtractor.fromGCValueToString(fieldValue)
  lazy val isId: Boolean              = field.name == "id"

  def updateValue(value: Any): NodeSelector = {
    val unwrapped = value match {
      case Some(x) => x
      case x       => x
    }

    val newGCValue = GCAnyConverter(field.typeIdentifier, isList = false).toGCValue(unwrapped).get
    this.copy(fieldValue = newGCValue)
  }
}
