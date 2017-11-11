package cool.graph.client.mutations.definitions

import cool.graph.client.schema.InputTypesBuilder
import cool.graph.shared.models.{Model, Project}
import cool.graph.{ArgumentSchema, CreateOrUpdateMutationDefinition, SchemaArgument}
import sangria.schema.Argument

case class CreateDefinition(argumentSchema: ArgumentSchema, project: Project, inputTypesBuilder: InputTypesBuilder) extends CreateOrUpdateMutationDefinition {

  val argumentGroupName = "Create"

  override def getSangriaArguments(model: Model): List[Argument[Any]] = inputTypesBuilder.getSangriaArgumentsForCreate(model)

  override def getRelationArguments(model: Model): List[SchemaArgument] = inputTypesBuilder.cachedRelationalSchemaArguments(model, omitRelation = None)
  override def getScalarArguments(model: Model): List[SchemaArgument]   = inputTypesBuilder.computeScalarSchemaArgumentsForCreate(model)
}
