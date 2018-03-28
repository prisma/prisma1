package com.prisma.api.connector

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.{Field, Model, Relation}

import scala.collection.immutable.Seq

object Types {
  type DataItemFilterCollection = Seq[_ >: Seq[Any] <: Any]
  //  type UserData                 = Map[String, Option[Any]]
}

case class ScalarListValue(
    nodeId: String,
    position: Int,
    value: Any
)
case class ResolverResult(
    items: Seq[DataItem],
    hasNextPage: Boolean = false,
    hasPreviousPage: Boolean = false,
    parentModelId: Option[String] = None
)

case class ResolverResultNew[T](
    nodes: Vector[T],
    hasNextPage: Boolean,
    hasPreviousPage: Boolean,
    parentModelId: Option[IdGCValue] = None
)

case class RelationResult(
    relations: Vector[RelationNode],
    hasNextPage: Boolean,
    hasPreviousPage: Boolean
)

case class ModelCounts(countsMap: Map[Model, Int]) {
  def countForName(name: String): Int = {
    val model = countsMap.keySet.find(_.name == name).getOrElse(sys.error(s"No count found for model $name"))
    countsMap(model)
  }
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
object QueryArguments {
  def empty = QueryArguments(None, None, None, None, None, None, None)

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
    filterName: String = "",
    relatedFilterElement: Option[FilterElementRelation] = None
)

case class FilterElementRelation(
    fromModel: Model,
    toModel: Model,
    relation: Relation,
    filter: DataItemFilterCollection
)
