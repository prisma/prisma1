package cool.graph.deploy.migration

import cool.graph.shared.models.Project
import org.scalactic.Or
import sangria.ast.Document

trait DesiredProjectInferer {
  def infer(graphQlSdl: Document): Project Or ProjectSyntaxError
}

sealed trait ProjectSyntaxError
case class RelationDirectiveNeeded(type1: String, type1Fields: Vector[String], type2: String, type2Fields: Vector[String]) extends ProjectSyntaxError

object DesiredProjectInferer {
  def apply() = new DesiredProjectInferer {
    override def infer(graphQlSdl: Document) = DesiredProjectInfererImpl(graphQlSdl).infer()
  }
}

case class DesiredProjectInfererImpl(
    graphQlSdl: Document
) {
  def infer(): Project Or ProjectSyntaxError = {
    ???
  }
}
