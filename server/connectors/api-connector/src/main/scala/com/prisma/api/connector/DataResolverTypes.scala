package com.prisma.api.connector

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.gc_values.{GCValue, IdGCValue}
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Field, Model, Relation}

import scala.collection.immutable.Seq

object Types {
  type DataItemFilterCollection = Seq[_ >: Seq[Any] <: Any] //todo
  //  type UserData                 = Map[String, Option[Any]]
}

case class ScalarListElement(nodeId: Id, position: Int, value: GCValue)

case class ResolverResult[T](
    nodes: Vector[T],
    hasNextPage: Boolean,
    hasPreviousPage: Boolean,
    parentModelId: Option[IdGCValue] = None
)

case class QueryArguments(
    skip: Option[Int],
    after: Option[String],
    first: Option[Int],
    before: Option[String],
    last: Option[Int],
    filter: Option[DataItemFilterCollection],
    orderBy: Option[OrderBy]
)
object QueryArguments {
  def empty = QueryArguments(skip = None, after = None, first = None, before = None, last = None, filter = None, orderBy = None)
  def filterOnly(filter: Option[DataItemFilterCollection]) =
    QueryArguments(skip = None, after = None, first = None, before = None, last = None, filter = filter, orderBy = None)
}

object SortOrder extends Enumeration {
  type SortOrder = Value
  val Asc: SortOrder.Value  = Value("asc")
  val Desc: SortOrder.Value = Value("desc")
}

case class OrderBy(
    field: Field,
    sortOrder: SortOrder.Value
)

case class FilterElement(
    key: String,
    value: Any,
    field: Option[Field] = None,
    filterName: String = ""
)

case class FinalValueFilter(
    key: String,
    value: GCValue,
    field: Field,
    filterName: String = ""
)

case class FinalRelationFilter(
    key: String,
    value: Any,
    field: Field,
    filterName: String = ""
)

case class TransitiveRelationFilter(
    field: Field,
    fromModel: Model,
    toModel: Model,
    relation: Relation,
    filterName: String = "",
    nestedFilter: DataItemFilterCollection
)
