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

    def removeLastEdge: Path = edges match {
      case x if x.isEmpty => sys.error("Don't call this on an empty path")
      case x              => copy(root, edges.dropRight(1))
    }

    def prepend(edge: Edge): Path = copy(root, edge +: edges)

    def pretty: String =
      s"Where: ${root.model.name}, ${root.field.name}, ${root.fieldValueAsString} |  " + edges
        .map(edge => s"${edge.parent.name}<->${edge.child.name}")
        .mkString(" ")

    def lastModel = edges match {
      case x if x.isEmpty => root.model
      case x              => x.last.child
    }

    def lastRelation = edges match {
      case x if x.isEmpty => None
      case x              => Some(x.last.relation)
    }
  }
  object Path {
    def empty(where: NodeSelector) = Path(where, List.empty)
  }

  def getEdge(project: Project, model: Model, field: Field): Edge =
    ModelEdge(model, field, field.relatedModel(project.schema).get, field.relatedField(project.schema), field.relation.get)

  def collectPaths(project: Project,
                   where: NodeSelector,
                   startModel: Model,
                   seenModels: List[Model] = List.empty,
                   seenRelations: List[Relation] = List.empty): List[Path] = {
    startModel.cascadingRelationFields.filter(relationField => !seenRelations.contains(relationField.relation.get)) match {
      case x if x.isEmpty =>
        List(Path.empty(where))

      case x =>
        x.flatMap { field =>
          val edge = getEdge(project, startModel, field)
          if (seenModels.contains(edge.child) || edge.relation.bothSidesCascade) throw APIErrors.CascadingDeletePathLoops()

          val childPaths = collectPaths(project, where, edge.child, seenModels :+ edge.child, seenRelations :+ edge.relation)
          childPaths.map(path => path.prepend(edge))
        }
    }
  }
}
