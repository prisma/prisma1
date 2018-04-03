package com.prisma.api.connector

import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models.{Field, Model, Project, Relation}

trait Edge {
  def parent: Model
  def parentField: Field
  def parentRelationSide: RelationSide = parentField.relationSide.get
  def child: Model
  def childField: Option[Field]
  def childRelationSide: RelationSide = parentField.oppositeRelationSide.get
  def relation: Relation
  def toNodeEdge(where: NodeSelector): NodeEdge = {
    NodeEdge(parent, parentField, child, childField, where, relation)
  }
}
case class ModelEdge(parent: Model, parentField: Field, child: Model, childField: Option[Field], relation: Relation)                          extends Edge
case class NodeEdge(parent: Model, parentField: Field, child: Model, childField: Option[Field], childWhere: NodeSelector, relation: Relation) extends Edge

//  case class NodePath(root: NodeSelector, edges: List[Edge], last: NodeEdge)

case class Path(root: NodeSelector, edges: List[Edge]) {

  def relations                    = edges.map(_.relation)
  def models                       = root.model +: edges.map(_.child)
  def otherCascadingRelationFields = lastModel.cascadingRelationFields.filter(relationField => !relations.contains(relationField.relation.get))
  def lastEdge                     = edges.lastOption
  def lastEdge_!                   = edges.last
  def lastRelation_!               = lastRelation.get
  def parentSideOfLastEdge         = lastEdge_!.parentRelationSide
  def childSideOfLastEdge          = lastEdge_!.childRelationSide

  def removeLastEdge: Path = edges match {
    case Nil => sys.error("Don't call this on an empty path")
    case _   => copy(root, edges.dropRight(1))
  }

  def appendCascadingEdge(project: Project, field: Field): Path = {
    val edge = ModelEdge(lastModel, field, field.relatedModel(project.schema).get, field.relatedField(project.schema), field.relation.get)
    if (edge.relation.bothSidesCascade || models.contains(edge.child)) throw APIErrors.CascadingDeletePathLoops()
    copy(root, edges :+ edge)
  }

  def appendEdge(project: Project, field: Field): Path = {
    val edge = ModelEdge(lastModel, field, field.relatedModel(project.schema).get, field.relatedField(project.schema), field.relation.get)
    copy(root, edges :+ edge)
  }

  def append(edge: Edge): Path = copy(root, edges :+ edge)

  def extend(project: Project, field: Field, nestedMutation: NestedMutation): Path = {
    nestedMutation match {
      case x: NestedWhere =>
        this.append(NodeEdge(lastModel, field, field.relatedModel(project.schema).get, field.relatedField(project.schema), x.where, field.relation.get))
      case _ => this.append(ModelEdge(lastModel, field, field.relatedModel(project.schema).get, field.relatedField(project.schema), field.relation.get))
    }
  }

  def lastModel = edges match {
    case Nil => root.model
    case x   => x.last.child
  }

  def lastRelation = edges match {
    case Nil => None
    case x   => Some(x.last.relation)
  }

  def pretty: String =
    s"Where: ${root.model.name}, ${root.field.name}, ${root.fieldValueAsString} |  " + edges
      .map(edge => s"${edge.parent.name}<->${edge.child.name}")
      .mkString(" ")

  def updatedRoot(args: CoolArgs): Path = {
    val whereFieldValue = args.raw.get(root.field.name)
    val updatedWhere    = whereFieldValue.map(root.updateValue).getOrElse(root)
    this.copy(root = updatedWhere)
  }

  def lastEdgeToNodeEdge(where: NodeSelector): Path = this.copy(edges = removeLastEdge.edges :+ lastEdge_!.toNodeEdge(where))

  def lastEdgeToNodeEdgeIfNecessary(nested: NestedMutation): Path = nested match {
    case x: NestedWhere => this.copy(edges = removeLastEdge.edges :+ lastEdge_!.toNodeEdge(x.where))
    case _              => this
  }

  def relationFieldsNotOnPathOnLastModel = lastModel.relationFields.filter(f => f.relation != lastRelation)
}

object Path {
  def empty(where: NodeSelector) = Path(where, List.empty)

  def collectCascadingPaths(project: Project, path: Path): Vector[Path] = path.otherCascadingRelationFields match {
    case Nil   => Vector(path)
    case edges => edges.flatMap(field => collectCascadingPaths(project, path.appendCascadingEdge(project, field))).toVector
  }

}
