package com.prisma.api.database

import com.prisma.api.database.mutactions.ClientSqlMutaction
import com.prisma.api.mutations.NodeSelector
import com.prisma.shared.models.{Field, Model, Project, Relation}

object CascadingDeletes {

  case class ModelsWithRelation(parent: Model, child: Model, relation: Relation)

  case class Path(where: NodeSelector, mwrs: List[ModelsWithRelation]) {
    def cutOne: Path = mwrs match {
      case x if x.isEmpty => sys.error("Dont call this on an empty path")
      case x              => copy(where, mwrs.drop(1))
    }
    def prepend(mwr: ModelsWithRelation): Path = copy(where, mwr +: mwrs)
    def pretty: String =
      s"Where: ${where.model.name}, ${where.field.name}, ${where.fieldValueAsString} |  " + mwrs
        .map(mwr => s"${mwr.parent.name}<->${mwr.child.name}")
        .mkString(" ")
    def detectCircle(path: List[ModelsWithRelation] = this.mwrs, seen: List[Model] = List.empty): Unit = {
      path match {
        case x if x.isEmpty                                 =>
        case x if x.nonEmpty && seen.contains(x.head.child) => sys.error("Circle")
        case head :: Nil if head.parent == head.child       => sys.error("Circle")
        case head :: tail                                   => detectCircle(tail, seen :+ head.parent)
      }
    }
  }
  object Path {
    def empty(where: NodeSelector) = Path(where, List.empty)
  }

  def getMWR(project: Project, model: Model, field: Field): ModelsWithRelation =
    ModelsWithRelation(model, field.relatedModel(project.schema).get, field.relation.get)

  def collectPaths(project: Project, where: NodeSelector, startNode: Model, excludes: List[Relation] = List.empty): List[Path] = {
    val cascadingRelationFields = startNode.cascadingRelationFields
    val res = cascadingRelationFields.flatMap { field =>
      val mwr = getMWR(project, startNode, field)
      if (excludes.contains(mwr.relation)) {
        List(Path.empty(where))
      } else {
        val childPaths = collectPaths(project, where, mwr.child, excludes :+ mwr.relation)
        childPaths.map(path => path.prepend(mwr))
      }
    }
    val distinct = res.distinct
    distinct.map(path => path.detectCircle())
    distinct
  }

  def generateCascadingDeleteMutactions(project: Project, where: NodeSelector): List[ClientSqlMutaction] = {

    val paths: List[Path] = collectPaths(project, where, where.model)

    paths.map(CascadingDeleteRelationMutactions(project, _))
  }
}
