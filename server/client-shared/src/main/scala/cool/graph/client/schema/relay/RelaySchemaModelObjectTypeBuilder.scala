package cool.graph.client.schema.relay

import cool.graph.DataItem
import cool.graph.client.database.DeferredTypes.{CountManyModelDeferred, CountToManyDeferred, RelayConnectionOutputType}
import cool.graph.client.database.{ConnectionParentElement, IdBasedConnection, IdBasedConnectionDefinition}
import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.client.{SangriaQueryArguments, UserContext}
import cool.graph.shared.models
import sangria.ast.{Argument => _}
import sangria.schema._
import scaldi.Injector

class RelaySchemaModelObjectTypeBuilder(project: models.Project, nodeInterface: Option[InterfaceType[UserContext, DataItem]] = None, modelPrefix: String = "")(
    implicit inj: Injector)
    extends SchemaModelObjectTypesBuilder[RelayConnectionOutputType](project, nodeInterface, modelPrefix, withRelations = true) {

  val modelConnectionTypes = includedModels
    .map(model => (model.name, modelToConnectionType(model).connectionType))
    .toMap

  val modelEdgeTypes = includedModels
    .map(model => (model.name, modelToConnectionType(model).edgeType))
    .toMap

  def modelToConnectionType(model: models.Model): IdBasedConnectionDefinition[UserContext, IdBasedConnection[DataItem], DataItem] = {
    IdBasedConnection.definition[UserContext, IdBasedConnection, DataItem](
      name = modelPrefix + model.name,
      nodeType = modelObjectTypes(model.name),
      connectionFields = List(
        sangria.schema.Field(
          "count",
          IntType,
          Some("Count of filtered result set without considering pagination arguments"),
          resolve = ctx => {
            val countArgs = ctx.value.parent.args.map(args => SangriaQueryArguments.createSimpleQueryArguments(None, None, None, None, None, args.filter, None))

            ctx.value.parent match {
              case ConnectionParentElement(Some(nodeId), Some(field), _) =>
                CountToManyDeferred(field, nodeId, countArgs)
              case _ =>
                CountManyModelDeferred(model, countArgs)
            }
          }
        ))
    )
  }

  override def resolveConnection(field: models.Field): OutputType[Any] = {
    field.isList match {
      case true  => modelConnectionTypes(field.relatedModel(project).get.name)
      case false => modelObjectTypes(field.relatedModel(project).get.name)
    }
  }
}
