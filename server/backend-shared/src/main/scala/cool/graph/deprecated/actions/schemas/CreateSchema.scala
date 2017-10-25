package cool.graph.deprecated.actions.schemas

import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.shared.models.{Model, Project}
import sangria.schema._
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global

class CreateSchema[ManyDataItemType](model: Model, project: Project, modelObjectTypes: SchemaModelObjectTypesBuilder[ManyDataItemType])(implicit inj: Injector)
    extends Injectable {

  val createdModelField: Field[ActionUserContext, Unit] = Field(
    "createdNode",
    description = Some("The newly created node"),
    fieldType = modelObjectTypes.modelObjectTypes(model.name),
    resolve = (ctx) => {
      ctx.ctx.dataResolver.resolveByUnique(model, "id", ctx.ctx.nodeId) map (_.get)
    }
  )

  def build(): Schema[ActionUserContext, Unit] = {
    val Query = ObjectType(
      "Query",
      List(createdModelField)
    )

    Schema(Query)
  }
}
