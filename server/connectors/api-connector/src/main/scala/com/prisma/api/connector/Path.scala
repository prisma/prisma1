package com.prisma.api.connector

import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models._

trait Edge {
  def parent: Model = parentField.model
  def parentField: RelationField
  def columnForParentRelationSide: String       = relation.columnForRelationSide(parentRelationSide)
  def parentRelationSide: RelationSide          = parentField.relationSide
  def child: Model                              = parentField.relatedModel_!
  def childField: RelationField                 = parentField.relatedField
  def columnForChildRelationSide: String        = relation.columnForRelationSide(childRelationSide)
  def childRelationSide: RelationSide           = parentField.oppositeRelationSide
  def relation: Relation                        = parentField.relation
  def toNodeEdge(where: NodeSelector): NodeEdge = NodeEdge(parentField, where)
}

case class ModelEdge(parentField: RelationField)                          extends Edge
case class NodeEdge(parentField: RelationField, childWhere: NodeSelector) extends Edge

case class UnqualifiedPath(edges: Vector[Edge])
object UnqualifiedPath {
  val empty = UnqualifiedPath(Vector.empty)
}

case class Path(root: NodeSelector, edges: List[Edge]) {

  def relations: List[Relation]                         = edges.map(_.relation)
  def models: List[Model]                               = root.model +: edges.map(_.child)
  def otherCascadingRelationFields: List[RelationField] = lastModel.cascadingRelationFields.filter(relationField => !relations.contains(relationField.relation))
  def lastEdge: Option[Edge]                            = edges.lastOption
  def lastEdge_! : Edge                                 = edges.last
  def lastRelation_! : Relation                         = lastRelation.get
  def parentSideOfLastEdge: RelationSide                = lastEdge_!.parentRelationSide
  def childSideOfLastEdge: RelationSide                 = lastEdge_!.childRelationSide

  def columnForParentSideOfLastEdge: String = lastEdge_!.columnForParentRelationSide
  def columnForChildSideOfLastEdge: String  = lastEdge_!.columnForChildRelationSide

  def removeLastEdge: Path = edges match {
    case Nil => sys.error("Don't call this on an empty path")
    case _   => copy(root, edges.dropRight(1))
  }

  def appendCascadingEdge(field: RelationField): Path = {
    val edge = ModelEdge(field)
    if (edge.relation.bothSidesCascade || models.contains(edge.child)) throw APIErrors.CascadingDeletePathLoops()
    copy(root, edges :+ edge)
  }

  def appendEdge(field: RelationField): Path = copy(root, edges :+ ModelEdge(field))
  def append(edge: Edge): Path               = copy(root, edges :+ edge)

  def lastModel: Model = edges match {
    case Nil => root.model
    case x   => x.last.child
  }

  def lastRelation: Option[Relation] = edges match {
    case Nil => None
    case x   => Some(x.last.relation)
  }

  def lastCreateWhere_! : NodeSelector = if (edges.isEmpty) root else lastEdge_!.asInstanceOf[NodeEdge].childWhere

  def pretty: String =
    s"Where: ${root.model.name}, ${root.field.name}, ${root.value} |  " + edges
      .map(edge => s"${edge.parent.name}<->${edge.child.name}")
      .mkString(" ")

  def lastEdgeToNodeEdge(where: NodeSelector): Path = this.copy(edges = removeLastEdge.edges :+ lastEdge_!.toNodeEdge(where))

  def relationFieldsNotOnPathOnLastModel: List[RelationField] = lastModel.relationFields.filter { f =>
    lastEdge match {
      case Some(edge) => edge.childField != f
      case None       => true
    }
  }
}

object Path {
  def empty(where: NodeSelector) = Path(where, List.empty)

  def collectCascadingPaths(path: Path): Vector[Path] = path.otherCascadingRelationFields match {
    case Nil   => Vector(path)
    case edges => edges.flatMap(field => collectCascadingPaths(path.appendCascadingEdge(field))).toVector
  }

}
