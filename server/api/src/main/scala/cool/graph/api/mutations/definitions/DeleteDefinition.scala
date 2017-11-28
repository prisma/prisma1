package cool.graph.api.mutations.definitions

import cool.graph.api.schema.{SchemaArgument, SchemaBuilderUtils}
import cool.graph.shared.models.{Model, Project}

case class DeleteDefinition(project: Project) extends ClientMutationDefinition {

  val argumentGroupName = "Delete"

  override def getSchemaArguments(model: Model): List[SchemaArgument] = {
    val idField = model.getFieldByName_!("id")
    List(
      SchemaArgument(idField.name, SchemaBuilderUtils.mapToRequiredInputType(idField), idField.description, idField)
    )
  }
}
