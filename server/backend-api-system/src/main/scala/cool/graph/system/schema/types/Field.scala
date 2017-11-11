package cool.graph.system.schema.types

import cool.graph.GCDataTypes.GCStringConverter
import cool.graph.Types.Id
import cool.graph.shared.models
import cool.graph.system.SystemUserContext
import cool.graph.system.schema.types.Model.ModelContext
import cool.graph.system.schema.types.Relation.RelationContext
import sangria.relay.Node
import sangria.schema._

object _Field {
  case class FieldContext(project: models.Project, field: models.Field) extends Node {
    def id: Id = field.id
  }

  lazy val Type: ObjectType[SystemUserContext, FieldContext] = ObjectType(
    "Field",
    "This is a field",
    interfaces[SystemUserContext, FieldContext](nodeInterface),
    () =>
      idField[SystemUserContext, FieldContext] ::
        fields[SystemUserContext, FieldContext](
        Field("name", StringType, resolve = _.value.field.name),
        Field("typeIdentifier", StringType, resolve = _.value.field.typeIdentifier.toString),
        Field("description", OptionType(StringType), resolve = _.value.field.description),
        Field("isRequired", BooleanType, resolve = _.value.field.isRequired),
        Field("isList", BooleanType, resolve = _.value.field.isList),
        Field("isUnique", BooleanType, resolve = _.value.field.isUnique),
        Field("isSystem", BooleanType, resolve = _.value.field.isSystem),
        Field("isReadonly", BooleanType, resolve = _.value.field.isReadonly),
        Field("enum", OptionType(OurEnumType), resolve = _.value.field.enum),
        Field("constraints", ListType(FieldConstraintType), resolve = _.value.field.constraints),
        Field(
          "defaultValue",
          OptionType(StringType),
          resolve =
            x => x.value.field.defaultValue.flatMap(dV => GCStringConverter(x.value.field.typeIdentifier, x.value.field.isList).fromGCValueToOptionalString(dV))
        ),
        Field("relation", OptionType(RelationType), resolve = ctx => {
          ctx.value.field.relation
            .map(relation => RelationContext(ctx.value.project, relation))
        }),
        Field(
          "model",
          OptionType(ModelType),
          resolve = ctx => {
            val project = ctx.value.project
            project.getModelByFieldId(ctx.value.id).map(model => ModelContext(project, model))
          }
        ),
        Field(
          "relatedModel",
          OptionType(ModelType),
          resolve = ctx => {
            val project = ctx.value.project
            project.getRelatedModelForField(ctx.value.field).map(model => ModelContext(project, model))
          }
        ),
        Field(
          "relationSide",
          OptionType(
            EnumType(
              "RelationSide",
              None,
              List(
                EnumValue(models.RelationSide.A.toString, value = models.RelationSide.A),
                EnumValue(models.RelationSide.B.toString, value = models.RelationSide.B)
              )
            )),
          resolve = _.value.field.relationSide
        ),
        Field(
          "reverseRelationField",
          OptionType(FieldType),
          resolve = ctx => {
            val project = ctx.value.project
            project.getReverseRelationField(ctx.value.field).map(field => FieldContext(project, field))
          }
        )
    )
  )
}
