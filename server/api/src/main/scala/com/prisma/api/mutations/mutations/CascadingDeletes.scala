package com.prisma.api.mutations.mutations

import com.prisma.api.mutations.NodeSelector
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models.{Field, Model, Project, Relation}

object CascadingDeletes {

  trait Edge {
    def parent: Model
    def parentField: Field
    def parentRelationSide: RelationSide = parentField.relationSide.get
    def child: Model
    def childField: Option[Field]
    def childRelationSide: RelationSide = parentField.oppositeRelationSide.get
    def relation: Relation
  }
  case class ModelEdge(parent: Model, parentField: Field, child: Model, childField: Option[Field], relation: Relation)                          extends Edge
  case class NodeEdge(parent: Model, parentField: Field, child: Model, childField: Option[Field], childWhere: NodeSelector, relation: Relation) extends Edge

  case class Path(root: NodeSelector, edges: List[Edge]) {

    def relations                    = edges.map(_.relation)
    def models                       = root.model +: edges.map(_.child)
    def otherCascadingRelationFields = lastModel.cascadingRelationFields.filter(relationField => relations.contains(relationField.relation.get))

    def removeLastEdge: Path = edges match {
      case Nil => sys.error("Don't call this on an empty path")
      case _   => copy(root, edges.dropRight(1))
    }

    def cascadeAppend(edge: Edge): Path = {
      if (models.contains(edge.child)) throw APIErrors.CascadingDeletePathLoops()
      copy(root, edges :+ edge)
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
  }
  object Path { def empty(where: NodeSelector) = Path(where, List.empty) }

  def cascadingEdge(project: Project, model: Model, field: Field): Edge = {
    val edge = ModelEdge(model, field, field.relatedModel(project.schema).get, field.relatedField(project.schema), field.relation.get)
    if (edge.relation.bothSidesCascade) throw APIErrors.CascadingDeletePathLoops()
    edge
  }

  def collectPaths(project: Project, path: Path): List[Path] = path.otherCascadingRelationFields match {
    case Nil => List(path)
    case x   => x.flatMap(field => collectPaths(project, path.cascadeAppend(cascadingEdge(project, path.lastModel, field))))
  }
}
