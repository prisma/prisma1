package cool.graph.deprecated.actions.schemas

import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.shared.models.{Model, Project}
import sangria.schema._
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global

class DeleteSchema[ManyDataItemType](model: Model, project: Project, modelObjectTypes: SchemaModelObjectTypesBuilder[ManyDataItemType])(implicit inj: Injector)
    extends Injectable {

  val deletedModelField: Field[ActionUserContext, Unit] = Field(
    "deletedNode",
    description = Some("The deleted model"),
    fieldType = modelObjectTypes.modelObjectTypes(model.name),
    resolve = (ctx) => ctx.ctx.dataResolver.resolveByUnique(model, "id", ctx.ctx.nodeId) map (_.get)
  )

  val mutationFieldType: ObjectType[Unit, MutationMetaData] = ObjectType(
    model.name,
    description = "Mutation meta information",
    fields = fields[Unit, MutationMetaData](
      Field("id", fieldType = IDType, description = Some("Mutation id for logging purposes"), resolve = _.value.id),
      Field("type", fieldType = StringType, description = Some("Type of the mutation"), resolve = _.value._type)
    )
  )

  val mutationField: Field[ActionUserContext, Unit] = Field(
    "mutation",
    description = Some("Mutation meta information"),
    fieldType = mutationFieldType,
    resolve = _.ctx.mutation
  )

  def build(): Schema[ActionUserContext, Unit] = {
    val Query = ObjectType(
      "Query",
      List(deletedModelField)
    )

    Schema(Query)
  }
}
