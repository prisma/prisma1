package cool.graph.deploy.migration

import sangria.ast.Document

trait RenameInferer {
  def infer(graphQlSdl: Document): Renames
}
