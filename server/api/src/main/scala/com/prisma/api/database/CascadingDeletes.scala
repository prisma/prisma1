package com.prisma.api.database

import com.prisma.api.database.mutactions.ClientSqlMutaction
import com.prisma.api.database.mutactions.mutactions.CascadingDeleteRelationMutactions
import com.prisma.api.mutations.NodeSelector
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.{Field, Model, Project, Relation}

object CascadingDeletes {

  case class Edge(parent: Model, child: Model, relation: Relation)

  case class Path(where: NodeSelector, edges: List[Edge]) {

    def cutOne: Path = edges match {
      case x if x.isEmpty => sys.error("Dont call this on an empty path")
      case x              => copy(where, edges.dropRight(1))
    }

    def prepend(edge: Edge): Path = copy(where, edge +: edges)

    def pretty: String =
      s"Where: ${where.model.name}, ${where.field.name}, ${where.fieldValueAsString} |  " + edges
        .map(edge => s"${edge.parent.name}<->${edge.child.name}")
        .mkString(" ")

    def lastModel = edges match {
      case x if x.isEmpty => where.model
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
    Edge(model, field.relatedModel(project.schema).get, field.relation.get)

  def collectPaths(project: Project, where: NodeSelector, startModel: Model, seen: List[Model] = List.empty): List[Path] = {
    startModel.cascadingRelationFields match {
      case x if x.isEmpty =>
        List(Path.empty(where))

      case x =>
        x.flatMap { field =>
          val edge = getEdge(project, startModel, field)
          if (seen.contains(edge.child)) throw APIErrors.CascadingDeletePathLoops()

          val childPaths = collectPaths(project, where, edge.child, seen :+ edge.child)
          childPaths.map(path => path.prepend(edge))
        }
    }
  }

  def generateCascadingDeleteMutactions(project: Project, where: NodeSelector): List[ClientSqlMutaction] = {
    def getMutactions(paths: List[Path]): List[ClientSqlMutaction] = {
      paths.filter(_.edges.nonEmpty) match {
        case x if x.isEmpty =>
          List.empty

        case x =>
          val maxPathLength     = x.map(_.edges.length).max
          val longestPaths      = x.filter(_.edges.lengthCompare(maxPathLength) == 0)
          val longestMutactions = longestPaths.map(CascadingDeleteRelationMutactions(project, _))
          val shortenedPaths    = longestPaths.map(_.cutOne)
          val newPaths          = x.filter(_.edges.lengthCompare(maxPathLength) < 0) ++ shortenedPaths

          longestMutactions ++ getMutactions(newPaths)
      }
    }

    val paths: List[Path] = collectPaths(project, where, where.model)
    getMutactions(paths)
  }
}
