package cool.graph

import cool.graph
import cool.graph.Types.{DataItemFilterCollection, UserData}
import cool.graph.shared.models.{Field, Model, Relation}
import sangria.relay.Node

object Types {
  type DataItemFilterCollection = Seq[_ >: Seq[Any] <: Any]
  type Id                       = String
  type UserData                 = Map[String, Option[Any]]
}

case class FilterElement(key: String,
                         value: Any,
                         field: Option[Field] = None,
                         filterName: String = "",
                         relatedFilterElement: Option[FilterElementRelation] = None)

case class FilterElementRelation(fromModel: Model, toModel: Model, relation: Relation, filter: DataItemFilterCollection)

case class DataItem(id: Types.Id, userData: UserData = Map.empty, typeName: Option[String] = None) extends Node {
  def apply(key: String): Option[Any]      = userData(key)
  def get[T](key: String): T               = userData(key).get.asInstanceOf[T]
  def getOption[T](key: String): Option[T] = userData.get(key).flatten.map(_.asInstanceOf[T])
}

object SortOrder extends Enumeration {
  type SortOrder = Value
  val Asc: graph.SortOrder.Value  = Value("asc")
  val Desc: graph.SortOrder.Value = Value("desc")
}

case class OrderBy(
    field: Field,
    sortOrder: SortOrder.Value
)

object DataItem {
  def fromMap(map: UserData): DataItem = {
    val id: String = map.getOrElse("id", None) match {
      case Some(value) => value.asInstanceOf[String]
      case None        => ""
    }

    DataItem(id = id, userData = map)
  }
}
