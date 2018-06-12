package com.prisma.api.connector

import com.prisma.gc_values.{CuidGCValue, ListGCValue, RootGCValue}

case class PrismaNode(id: CuidGCValue, data: RootGCValue, typeName: Option[String] = None)

object PrismaNode {
  def dummy: PrismaNode = PrismaNode(CuidGCValue(""), RootGCValue.empty)
}

case class PrismaNodeWithParent(parentId: CuidGCValue, prismaNode: PrismaNode)

case class RelationNode(a: CuidGCValue, b: CuidGCValue)
case class ScalarListValues(nodeId: CuidGCValue, value: ListGCValue)
