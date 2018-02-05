package com.prisma.api.database

import com.prisma.api.database.mutactions.ClientSqlMutaction
import com.prisma.api.database.mutactions.mutactions.CascadingDeleteRelationMutactions
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
    def lastModel = mwrs match {
      case x if x.isEmpty => where.model
      case x              => x.reverse.head.child
    }

    def lastRelation = mwrs match {
      case x if x.isEmpty => None
      case x              => Some(x.reverse.head.relation)
    }
  }
  object Path {
    def empty(where: NodeSelector) = Path(where, List.empty)
  }

  def getMWR(project: Project, model: Model, field: Field): ModelsWithRelation =
    ModelsWithRelation(model, field.relatedModel(project.schema).get, field.relation.get)

  def collectPaths(project: Project, where: NodeSelector, startModel: Model, excludes: List[Relation] = List.empty): List[Path] = {
    val otherRelationFields = startModel.cascadingRelationFields.filter(field => !excludes.contains(field.relation.get))

    otherRelationFields match {
      case x if x.isEmpty =>
        List(Path.empty(where))

      case x =>
        x.flatMap { field =>
          val mwr        = getMWR(project, startModel, field)
          val childPaths = collectPaths(project, where, mwr.child, excludes :+ mwr.relation)
          childPaths.map(path => path.prepend(mwr))
        }
    }
  }

  def generateCascadingDeleteMutactions(project: Project, where: NodeSelector): List[ClientSqlMutaction] = {
    val paths: List[Path] = collectPaths(project, where, where.model)
    paths.map(CascadingDeleteRelationMutactions(project, _))
  }
}
