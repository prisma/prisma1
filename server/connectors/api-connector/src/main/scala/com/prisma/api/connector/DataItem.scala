package com.prisma.api.connector

import com.prisma.gc_values.{ListGCValue, RootGCValue}
import com.prisma.shared.models.IdType.Id
import com.prisma.util.gc_value.GCValueExtractor

case class DataItem(id: Id, userData: Map[String, Option[Any]] = Map.empty, typeName: Option[String] = None) {
  def apply(key: String): Option[Any]      = userData(key)
  def get[T](key: String): T               = userData(key).get.asInstanceOf[T]
  def getOption[T](key: String): Option[T] = userData.get(key).flatten.map(_.asInstanceOf[T])
}

case class PrismaNode(id: Id, data: RootGCValue, typeName: Option[String] = None) {
  def toDataItem = DataItem(id, data.map.map { case (k, v) => (k, GCValueExtractor.fromGCValueToOption(v)) })
}

case class PrismaNodeWithParent(parentId: Id, prismaNode: PrismaNode)

case class RelationNode(id: Id, a: Id, b: Id)
case class ScalarListValues(nodeId: Id, value: ListGCValue)
