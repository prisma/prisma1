package cool.graph.client.mutations.definitions

import cool.graph.client.schema.InputTypesBuilder
import cool.graph.shared.models.{Model, Project}
import cool.graph.{ArgumentSchema, ClientMutationDefinition, SchemaArgument}
import sangria.schema.Argument

case class UpdateOrCreateDefinition(argumentSchema: ArgumentSchema, project: Project, inputTypesBuilder: InputTypesBuilder) extends ClientMutationDefinition {

  val argumentGroupName = "UpdateOrCreate"

  val createDefinition = CreateDefinition(argumentSchema, project, inputTypesBuilder)
  val updateDefinition = UpdateDefinition(argumentSchema, project, inputTypesBuilder)

  override def getSangriaArguments(model: Model): List[Argument[Any]] = {
    inputTypesBuilder.getSangriaArgumentsForUpdateOrCreate(model)
  }

  override def getSchemaArguments(model: Model): List[SchemaArgument] = ???
}
