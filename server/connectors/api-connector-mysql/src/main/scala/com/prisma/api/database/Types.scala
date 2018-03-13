package com.prisma.api.database

import com.prisma.api.connector.DataItem
import com.prisma.api.database.Types.{DataItemFilterCollection}
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Field, Model, Relation}
import sangria.relay.Node

object Types {
  type DataItemFilterCollection = Seq[_ >: Seq[Any] <: Any]
//  type UserData                 = Map[String, Option[Any]]
}

case class FilterElement(key: String,
                         value: Any,
                         field: Option[Field] = None,
                         filterName: String = "",
                         relatedFilterElement: Option[FilterElementRelation] = None)

case class FilterElementRelation(fromModel: Model, toModel: Model, relation: Relation, filter: DataItemFilterCollection)

case class ScalarListValue(nodeId: String, position: Int, value: Any)

object SortOrder extends Enumeration {
  type SortOrder = Value
  val Asc: SortOrder.Value  = Value("asc")
  val Desc: SortOrder.Value = Value("desc")
}

case class OrderBy(
    field: Field,
    sortOrder: SortOrder.Value
)
