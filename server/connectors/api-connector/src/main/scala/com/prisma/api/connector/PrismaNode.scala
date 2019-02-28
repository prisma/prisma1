package com.prisma.api.connector

import com.prisma.gc_values._
import com.prisma.shared.models.RelationField

case class PrismaNode(id: IdGCValue, data: RootGCValue, typeName: Option[String] = None) {
  def getToOneChild(relationField: RelationField): Option[PrismaNode] = data.map.get(relationField.name) match {
    case None              => None
    case Some(NullGCValue) => None
    case Some(value) =>
      Some(PrismaNode(value.asRoot.idFieldByName(relationField.relatedModel_!.idField_!.name), value.asRoot, Some(relationField.relatedModel_!.name)))
  }

  def getToManyChild(relationField: RelationField, where: NodeSelector): Option[PrismaNode] = data.map.get(relationField.name) match {
    case None                      => None
    case Some(NullGCValue)         => None
    case Some(ListGCValue(values)) => evaluateListGCValue(relationField, values, where)
    case x                         => sys.error("Checking for toMany child in PrismaNode returned unexpected result" + x)
  }

  private def evaluateListGCValue(relationField: RelationField, values: Vector[GCValue], where: NodeSelector) = {
    values.find(value => value.asRoot.map(where.field.name) == where.fieldGCValue) match {
      case Some(gc) =>
        Some(PrismaNode(gc.asRoot.idFieldByName(relationField.relatedModel_!.idField_!.name), gc.asRoot, Some(relationField.relatedModel_!.name)))
      case None => None
    }
  }

  def getIDAtPath(parentField: RelationField, path: Path): Option[IdGCValue] = PrismaNode.getNodeAtPath(Some(this), path.segments) match {
    case None =>
      None

    case Some(n) =>
      n.data.map.get(parentField.name) match {
        case Some(x: StringIdGCValue) => Some(x)
        case _                        => None
      }
  }
}

object PrismaNode {
  def dummy: PrismaNode = PrismaNode(StringIdGCValue.dummy, RootGCValue.empty)

  def getNodeAtPath(node: Option[PrismaNode], segments: List[PathSegment]): Option[PrismaNode] = (node, segments.headOption) match {
    case (nodeOption, None)                                       => nodeOption
    case (None, _)                                                => None
    case (Some(node), Some(ToOneSegment(rf)))                     => getNodeAtPath(node.getToOneChild(rf), segments.drop(1))
    case (Some(node), Some(ToManySegment(rf, where)))             => getNodeAtPath(node.getToManyChild(rf, where), segments.drop(1))
    case (Some(node), Some(ToManyFilterSegment(rf, whereFilter))) => ???
  }
}

case class PrismaNodeWithParent(parentId: IdGCValue, prismaNode: PrismaNode)

case class RelationNode(a: IdGCValue, b: IdGCValue)
case class ScalarListValues(nodeId: IdGCValue, value: ListGCValue)
