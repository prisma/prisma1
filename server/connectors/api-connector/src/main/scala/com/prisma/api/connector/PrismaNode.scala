package com.prisma.api.connector

import com.prisma.gc_values.{CuidGCValue, IdGcValue, ListGCValue, RootGCValue}

case class PrismaNode(id: IdGcValue, data: RootGCValue, typeName: Option[String] = None)

object PrismaNode {
  def dummy: PrismaNode = PrismaNode(CuidGCValue(""), RootGCValue.empty)
}

case class PrismaNodeWithParent(parentId: IdGcValue, prismaNode: PrismaNode)

case class RelationNode(a: IdGcValue, b: IdGcValue)
case class ScalarListValues(nodeId: IdGcValue, value: ListGCValue)
