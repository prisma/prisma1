package cool.graph.api.mutations.definitions

import cool.graph.api.schema.{InputTypesBuilder, SchemaArgument}
import cool.graph.shared.models.{Model, Project}
import sangria.schema.Argument

case class CreateDefinition(project: Project, inputTypesBuilder: InputTypesBuilder) extends CreateOrUpdateMutationDefinition {

  val argumentGroupName = "Create"

  override def getSangriaArguments(model: Model): List[Argument[Any]] = inputTypesBuilder.getSangriaArgumentsForCreate(model)

  override def getRelationArguments(model: Model): List[SchemaArgument] = inputTypesBuilder.cachedRelationalSchemaArguments(model, omitRelation = None)
  override def getScalarArguments(model: Model): List[SchemaArgument]   = inputTypesBuilder.computeScalarSchemaArgumentsForCreate(model)
}
