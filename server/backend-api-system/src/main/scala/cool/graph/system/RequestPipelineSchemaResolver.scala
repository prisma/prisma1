package cool.graph.system

import cool.graph.shared.models.FunctionBinding.FunctionBinding
import cool.graph.shared.models.RequestPipelineOperation.RequestPipelineOperation
import cool.graph.shared.models.{Model, Project, RequestPipelineOperation}
import cool.graph.system.migration.dataSchema.SchemaExport
import sangria.ast.ObjectTypeDefinition
import sangria.renderer.QueryRenderer

class RequestPipelineSchemaResolver {
  def resolve(project: Project, model: Model, binding: FunctionBinding, operation: RequestPipelineOperation): String = {

    val fields = operation match {
      case RequestPipelineOperation.CREATE => model.scalarFields
      case RequestPipelineOperation.UPDATE =>
        model.scalarFields.map(f =>
          f.name match {
            case "id" => f
            case _    => f.copy(isRequired = false)
        })
      case RequestPipelineOperation.DELETE => model.scalarFields.filter(_.name == "id")
    }

    val fieldDefinitions = fields
      .map(field => {
        SchemaExport.buildFieldDefinition(project, model, field)
      })
      .map(definition => definition.copy(directives = Vector.empty))
      .toVector

    val res =
      ObjectTypeDefinition(s"${model.name}Input", interfaces = Vector(), fields = fieldDefinitions.sortBy(_.name), directives = Vector(), comments = Vector())

    QueryRenderer.render(res)
  }
}
