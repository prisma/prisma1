package cool.graph.api.mutations.definitions

import cool.graph.shared.models.{Model, Project}
import sangria.schema.Argument

case class DeleteDefinition(project: Project) extends ClientMutationDefinition {
  override def getSangriaArguments(model: Model): List[Argument[Any]] = List.empty
}
