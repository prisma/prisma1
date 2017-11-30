package cool.graph.api.mutations.definitions

import cool.graph.api.schema.{InputTypesBuilder, SchemaArgument}
import cool.graph.shared.models.{Model, Project}
import sangria.schema.Argument

case class UpdateOrCreateDefinition(project: Project, inputTypesBuilder: InputTypesBuilder) extends ClientMutationDefinition {

  val argumentGroupName = "UpdateOrCreate"

  val createDefinition = CreateDefinition(project, inputTypesBuilder)
  val updateDefinition = UpdateDefinition(project, inputTypesBuilder)

  override def getSangriaArguments(model: Model): List[Argument[Any]] = {
    inputTypesBuilder.getSangriaArgumentsForUpdateOrCreate(model)
  }

  override def getSchemaArguments(model: Model): List[SchemaArgument] = ???
}
