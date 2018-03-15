package com.prisma.api.connector.mysql.database

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.shared.models.{Field, Model, Relation}

case class FilterElement(key: String,
                         value: Any,
                         field: Option[Field] = None,
                         filterName: String = "",
                         relatedFilterElement: Option[FilterElementRelation] = None)

case class FilterElementRelation(fromModel: Model, toModel: Model, relation: Relation, filter: DataItemFilterCollection)
