package cool.graph.deploy.migration

import cool.graph.shared.models.Project
import sangria.ast.Document

trait DesiredProjectInferer {
  def infer(graphQlSdl: Document): Project
}

object DesiredProjectInferer extends DesiredProjectInferer {
  override def infer(graphQlSdl: Document): Project = ???
}
