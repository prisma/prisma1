package com.prisma.api.connector.mysql.database

import com.prisma.api.connector.mysql.database.Types.DataItemFilterCollection
import com.prisma.shared.models.{Field, Model, Relation}

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
