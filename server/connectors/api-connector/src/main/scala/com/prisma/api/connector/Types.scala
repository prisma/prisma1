package com.prisma.api.connector

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.shared.models.Field

object Types {
  type DataItemFilterCollection = Seq[_ >: Seq[Any] <: Any]
  //  type UserData                 = Map[String, Option[Any]]
}

case class QueryArguments(
    skip: Option[Int],
    after: Option[String],
    first: Option[Int],
    before: Option[String],
    last: Option[Int],
    filter: Option[DataItemFilterCollection],
    orderBy: Option[OrderBy]
)

object SortOrder extends Enumeration {
  type SortOrder = Value
  val Asc: SortOrder.Value  = Value("asc")
  val Desc: SortOrder.Value = Value("desc")
}

case class OrderBy(
    field: Field,
    sortOrder: SortOrder.Value
)
