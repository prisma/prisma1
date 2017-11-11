package cool.graph.client.mutations.definitions

import cool.graph.client.SchemaBuilderUtils
import cool.graph.shared.models.{Model, Project}
import cool.graph.{ArgumentSchema, ClientMutationDefinition, SchemaArgument}

case class DeleteDefinition(argumentSchema: ArgumentSchema, project: Project) extends ClientMutationDefinition {

  val argumentGroupName = "Delete"

  override def getSchemaArguments(model: Model): List[SchemaArgument] = {
    val idField = model.getFieldByName_!("id")
    List(
      SchemaArgument(idField.name, SchemaBuilderUtils.mapToRequiredInputType(idField), idField.description, idField)
    )
  }
}
