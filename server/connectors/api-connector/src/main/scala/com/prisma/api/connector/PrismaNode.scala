package com.prisma.api.connector

import com.prisma.gc_values.{IdGCValue, ListGCValue, RootGCValue}

case class PrismaNode(id: IdGCValue, data: RootGCValue, typeName: Option[String] = None) {
//  def toDataItem = DataItem(id.value, data.map.map { case (k, v) => (k, GCValueExtractor.fromGCValueToOption(v)) })
}

object PrismaNode {
  def dummy: PrismaNode = PrismaNode(IdGCValue(""), RootGCValue.empty)
}

case class PrismaNodeWithParent(parentId: IdGCValue, prismaNode: PrismaNode)

case class RelationNode(id: IdGCValue, a: IdGCValue, b: IdGCValue)
case class ScalarListValues(nodeId: IdGCValue, value: ListGCValue)
