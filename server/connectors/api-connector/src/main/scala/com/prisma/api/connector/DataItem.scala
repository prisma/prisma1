package com.prisma.api.connector

import com.prisma.shared.models.IdType.Id
import sangria.relay.Node

case class DataItem(id: Id, userData: Map[String, Option[Any]] = Map.empty, typeName: Option[String] = None) extends Node {
  def apply(key: String): Option[Any]      = userData(key)
  def get[T](key: String): T               = userData(key).get.asInstanceOf[T]
  def getOption[T](key: String): Option[T] = userData.get(key).flatten.map(_.asInstanceOf[T])
}

object DataItem {
  def fromMap(map: Map[String, Option[Any]]): DataItem = {
    val id: String = map.getOrElse("id", None) match {
      case Some(value) => value.asInstanceOf[String]
      case None        => ""
    }

    DataItem(id = id, userData = map)
  }
}
