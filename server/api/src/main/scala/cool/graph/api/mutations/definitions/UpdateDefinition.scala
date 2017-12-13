package cool.graph.api.mutations.definitions

import cool.graph.api.schema.{InputTypesBuilder, SchemaArgument}
import cool.graph.shared.models.{Model, Project}
import sangria.schema.Argument

case class UpdateDefinition(project: Project, inputTypesBuilder: InputTypesBuilder) extends ClientMutationDefinition {
  override def getSangriaArguments(model: Model): List[Argument[Any]] = inputTypesBuilder.getSangriaArgumentsForUpdate(model)
}
