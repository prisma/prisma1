package com.prisma.api.connector

import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models._

trait Edge {
  def parent: Model = parentField.model
  def parentField: Field
  def columnForParentRelationSide: String       = relation.columnForRelationSide(parentRelationSide)
  def parentRelationSide: RelationSide          = parentField.relationSide.get
  def child: Model                              = parentField.relatedModel_!
  def childField: Option[Field]                 = parentField.relatedField
  def columnForChildRelationSide: String        = relation.columnForRelationSide(childRelationSide)
  def childRelationSide: RelationSide           = parentField.oppositeRelationSide.get
  def relation: Relation                        = parentField.relation_!
  def toNodeEdge(where: NodeSelector): NodeEdge = NodeEdge(parentField, where)
}
case class ModelEdge(parentField: Field)                          extends Edge
case class NodeEdge(parentField: Field, childWhere: NodeSelector) extends Edge
//  case class NodePath(root: NodeSelector, edges: List[Edge], last: NodeEdge)

case class Path(root: NodeSelector, edges: List[Edge]) {

  def relations: List[Relation]                             = edges.map(_.relation)
  def models: List[Model]                                   = root.model +: edges.map(_.child)
  def otherCascadingRelationFields: List[Field]             = lastModel.cascadingRelationFields.filter(relationField => !relations.contains(relationField.relation_!))
  def lastEdge: Option[Edge]                                = edges.lastOption
  def lastEdge_! : Edge                                     = edges.last
  def lastRelation_! : Relation                             = lastRelation.get
  def parentSideOfLastEdge: RelationSide                    = lastEdge_!.parentRelationSide
  def childSideOfLastEdge: RelationSide                     = lastEdge_!.childRelationSide
  def columnForParentSideOfLastEdge(schema: Schema): String = lastEdge_!.columnForParentRelationSide
  def columnForChildSideOfLastEdge(schema: Schema): String  = lastEdge_!.columnForChildRelationSide
  def removeLastEdge: Path                                  = if (edges.isEmpty) sys.error("Don't call this on an empty path") else copy(root, edges.dropRight(1))
  def appendEdge(field: Field): Path                        = copy(root, edges :+ ModelEdge(field))
  def append(edge: Edge): Path                              = copy(root, edges :+ edge)
  def lastModel: Model                                      = if (edges.isEmpty) root.model else edges.last.child
  def lastRelation: Option[Relation]                        = if (edges.isEmpty) None else Some(edges.last.relation)
  def lastCreateWhere_! : NodeSelector                      = if (edges.isEmpty) root else lastEdge_!.asInstanceOf[NodeEdge].childWhere

  def appendCascadingEdge(field: Field): Path = {
    val edge = ModelEdge(field)
    if (edge.relation.bothSidesCascade || models.contains(edge.child)) throw APIErrors.CascadingDeletePathLoops()
    copy(root, edges :+ edge)
  }

  def pretty: String =
    s"Where: ${root.model.name}, ${root.field.name}, ${root.fieldValueAsString} |  " + edges
      .map(edge => s"${edge.parent.name}<->${edge.child.name}")
      .mkString(" ")

  def lastEdgeToNodeEdge(where: NodeSelector): Path = this.copy(edges = removeLastEdge.edges :+ lastEdge_!.toNodeEdge(where))

  def relationFieldsNotOnPathOnLastModel: List[Field] = lastModel.relationFields.filter { f =>
    lastEdge match {
      case Some(edge) => !edge.childField.contains(f)
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
