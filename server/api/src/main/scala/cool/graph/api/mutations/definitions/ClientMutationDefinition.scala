package cool.graph.api.mutations.definitions

import cool.graph.api.schema.{SchemaArgument}
import cool.graph.shared.models.Model
import sangria.schema.Argument

trait ClientMutationDefinition {
  def argumentGroupName: String

  // TODO: there should be no need to override this one. It should be final. We should not override this one.
  def getSangriaArguments(model: Model): List[Argument[Any]] = {
    SchemaArgument.convertSchemaArgumentsToSangriaArguments(
      argumentGroupName + model.name,
      getSchemaArguments(model)
    )
  }

  def getSchemaArguments(model: Model): List[SchemaArgument]
}

trait CreateOrUpdateMutationDefinition extends ClientMutationDefinition {
  final def getSchemaArguments(model: Model): List[SchemaArgument] = getScalarArguments(model) ++ getRelationArguments(model)

  def getScalarArguments(model: Model): List[SchemaArgument]

  def getRelationArguments(model: Model): List[SchemaArgument]
}
