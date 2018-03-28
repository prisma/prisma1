package com.prisma.api.connector

import com.prisma.gc_values.{GraphQLIdGCValue, ListGCValue, RootGCValue}
import com.prisma.shared.models.IdType.Id
import com.prisma.util.gc_value.GCValueExtractor

case class DataItem(id: Id, userData: Map[String, Option[Any]] = Map.empty, typeName: Option[String] = None) {
  def apply(key: String): Option[Any]      = userData(key)
  def get[T](key: String): T               = userData(key).get.asInstanceOf[T]
  def getOption[T](key: String): Option[T] = userData.get(key).flatten.map(_.asInstanceOf[T])
}

case class PrismaNode(id: GraphQLIdGCValue, data: RootGCValue, typeName: Option[String] = None) {
  def toDataItem = DataItem(id.value, data.map.map { case (k, v) => (k, GCValueExtractor.fromGCValueToOption(v)) })
}

object PrismaNode {
  def dummy: PrismaNode = PrismaNode(GraphQLIdGCValue(""), RootGCValue.empty)
}

case class PrismaNodeWithParent(parentId: GraphQLIdGCValue, prismaNode: PrismaNode)

case class RelationNode(id: GraphQLIdGCValue, a: Id, b: Id)
case class ScalarListValues(nodeId: Id, value: ListGCValue)
