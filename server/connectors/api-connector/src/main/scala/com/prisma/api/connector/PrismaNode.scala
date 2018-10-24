package com.prisma.api.connector

import com.prisma.gc_values._
import com.prisma.shared.models.RelationField

case class PrismaNode(id: IdGCValue, data: RootGCValue, typeName: Option[String] = None) {
  def getToOneChild(relationField: RelationField): Option[PrismaNode] = data.map.get(relationField.name) match {
    case None              => None
    case Some(NullGCValue) => None
    case Some(value)       => Some(PrismaNode(value.asRoot.idField, value.asRoot, Some(relationField.relatedModel_!.name)))
  }

  def getToManyChild(relationField: RelationField, where: NodeSelector): Option[PrismaNode] = data.map.get(relationField.name) match {
    case None =>
      None

    case Some(NullGCValue) =>
      None

    case Some(ListGCValue(values)) =>
      values.find(value => value.asRoot.map(where.fieldName) == where.fieldGCValue) match {
        case Some(gc) => Some(PrismaNode(gc.asRoot.idField, gc.asRoot, Some(relationField.relatedModel_!.name)))
        case None     => None
      }

    case x =>
      sys.error("Checking for toMany child in PrismaNode returned unexpected result" + x)
  }
}

object PrismaNode {
  def dummy: PrismaNode = PrismaNode(CuidGCValue(""), RootGCValue.empty)
}

case class PrismaNodeWithParent(parentId: IdGCValue, prismaNode: PrismaNode)

case class RelationNode(a: IdGCValue, b: IdGCValue)
case class ScalarListValues(nodeId: IdGCValue, value: ListGCValue)
