package com.prisma.api.connector

import com.prisma.gc_values.{StringIdGCValue, GCValue, IdGCValue}
import com.prisma.shared.models.{Model, RelationField, ScalarField}

object NodeSelector {
  def forCuid(model: Model, id: String): NodeSelector              = NodeSelector(model, model.idField_!, StringIdGCValue(id))
  def forId(model: Model, gcValue: IdGCValue): NodeSelector        = NodeSelector(model, model.idField_!, gcValue)
  def forGCValue(model: Model, field: ScalarField, value: GCValue) = NodeSelector(model, field, value)
}

case class NodeSelector(model: Model, field: ScalarField,
: GCValue) {
  require(field.isUnique, s"NodeSelectors may be only instantiated for unique fields! ${field.name} on ${model.name} is not unique.")
  lazy val value         = fieldGCValue.value
  lazy val fieldName     = field.name
  lazy val isId: Boolean = field.name == "id"
}

object NodeAddress {
  def forId(model: Model, gCValue: IdGCValue, path: Path = Path.empty) = NodeAddress(NodeSelector.forId(model, gCValue), path)
}

case class NodeAddress(where: NodeSelector, path: Path = Path.empty) {
  def idValue: IdGCValue                                                   = where.fieldGCValue.asInstanceOf[IdGCValue]
  def newPath(newPath: Path): NodeAddress                                  = this.copy(path = newPath)
  def appendPath(rf: RelationField)                                        = newPath(this.path.append(rf))
  def appendPath(rf: RelationField, where: NodeSelector)                   = newPath(this.path.append(rf, where))
  def appendPath(rf: RelationField, where: NodeSelector, node: PrismaNode) = newPath(this.path.append(rf, NodeSelector.forId(where.model, node.id)))
  def appendPath(rf: RelationField, whereFilter: Option[Filter])           = newPath(this.path.append(rf, whereFilter))
}
