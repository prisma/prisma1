package cool.graph.deprecated.actions.schemas

import cool.graph.DataItem
import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.client.schema.simple.SimpleSchemaModelObjectTypeBuilder
import cool.graph.shared.models.{Model, Project}
import sangria.schema._
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global

class UpdateSchema[ManyDataItemType](model: Model,
                                     project: Project,
                                     modelObjectTypes: SchemaModelObjectTypesBuilder[ManyDataItemType],
                                     updatedFields: List[String],
                                     previousValues: DataItem)(implicit inj: Injector)
    extends Injectable {

  val updatedModelField: Field[ActionUserContext, Unit] = Field(
    "updatedNode",
    description = Some("The updated node"),
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

  val changedFieldsField: Field[ActionUserContext, Unit] = Field(
    "changedFields",
    description = Some("List of all names of the fields which changed"),
    fieldType = ListType(StringType),
    resolve = _ => updatedFields
  )

  val previousValuesField: Field[ActionUserContext, Unit] = Field(
    "previousValues",
    description = Some("Previous scalar values"),
    fieldType = new SimpleSchemaModelObjectTypeBuilder(project, withRelations = false, modelPrefix = "PreviousValues_")
      .modelObjectTypes(model.name),
    resolve = _ => previousValues
  )

  def build(): Schema[ActionUserContext, Unit] = {
    val Query = ObjectType(
      "Query",
      List(updatedModelField, changedFieldsField, previousValuesField)
    )

    Schema(Query)
  }
}
