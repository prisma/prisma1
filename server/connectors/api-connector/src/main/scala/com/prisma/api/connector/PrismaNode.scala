package com.prisma.api.connector

import com.prisma.gc_values._
import com.prisma.shared.models.RelationField

case class PrismaNode(id: IdGCValue, data: RootGCValue, typeName: Option[String] = None) {
  def toOneChild(relationField: RelationField): Option[PrismaNode] = data.map.get(relationField.name) match {
    case None              => None
    case Some(NullGCValue) => None
    case Some(value)       => Some(PrismaNode(CuidGCValue.dummy, value.asRoot, Some(relationField.relatedModel_!.name)))
  }

  def toManyChild(relationField: RelationField, where: NodeSelector): Option[PrismaNode] = data.map.get(relationField.name) match {
    case None =>
      None

    case Some(NullGCValue) =>
      None

    case Some(ListGCValue(values)) =>
      values.find(value => value.asRoot.map(where.fieldName) == where.fieldGCValue) match {
        case Some(gc) => Some(PrismaNode(CuidGCValue.dummy, gc.asRoot, Some(relationField.relatedModel_!.name)))
        case None     => None
      }
    case x => sys.error("Checking for toMany child in PrismaNode returned unexpected result" + x)
  }

  //helpers that are needed on PrismaNode (probably only for Mongo):
  // - taking a PrismaNode and a path and return whether its null
  // - taking a PrismaNode, a path and a value and check whether it exists
  // - taking a PrismaNode and a path and returning nested PrismaNodes at that Path

}

object PrismaNode {
  def dummy: PrismaNode = PrismaNode(CuidGCValue(""), RootGCValue.empty)
}

// Path
// consists of relationFieldNames and optionally a nodeSelector on the nested nodes

case class PrismaNodeWithParent(parentId: IdGCValue, prismaNode: PrismaNode)

case class RelationNode(a: IdGCValue, b: IdGCValue)
case class ScalarListValues(nodeId: IdGCValue, value: ListGCValue)
