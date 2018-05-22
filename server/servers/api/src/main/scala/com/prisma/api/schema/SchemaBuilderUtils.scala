package com.prisma.api.schema

import com.prisma.api.schema.CustomScalarTypes.{DateTimeType, JsonType}
import com.prisma.shared.models
import com.prisma.shared.models.{Model, Project, TypeIdentifier}
import sangria.schema._

object SchemaBuilderUtils {
  def mapToOptionalInputType(field: models.Field): InputType[Any] = {
    OptionInputType(mapToRequiredInputType(field))
  }

  def mapToRequiredInputType(field: models.Field): InputType[Any] = {
    assert(field.isScalar)

    val inputType: InputType[Any] = field.typeIdentifier match {
      case TypeIdentifier.String    => StringType
      case TypeIdentifier.Int       => IntType
      case TypeIdentifier.Float     => FloatType
      case TypeIdentifier.Boolean   => BooleanType
      case TypeIdentifier.GraphQLID => IDType
      case TypeIdentifier.DateTime  => DateTimeType
      case TypeIdentifier.Json      => JsonType
      case TypeIdentifier.Enum      => mapEnumFieldToInputType(field)
    }

    if (field.isList) {
      ListInputType(inputType)
    } else {
      inputType
    }
  }

  def mapEnumFieldToInputType(field: models.Field): EnumType[Any] = {
    require(field.typeIdentifier == TypeIdentifier.Enum, "This function must be called with Enum fields only!")
    val enum = field.enum.getOrElse(sys.error("A field with TypeIdentifier Enum must always have an enum."))
    EnumType(
      name = enum.name,
      description = None,
      values = enum.values.map(enumValue => EnumValue(enumValue, value = enumValue, description = None)).toList
    )
  }

  def mapToInputField(field: models.Field): List[InputField[_ >: Option[Seq[Any]] <: Option[Any]]] = {
    FilterArguments
      .getFieldFilters(field)
      .map({
        case FilterArgument(filterName, desc, true) =>
          InputField(field.name + filterName, OptionInputType(ListInputType(mapToRequiredInputType(field))), description = desc)

        case FilterArgument(filterName, desc, false) =>
          InputField(field.name + filterName, OptionInputType(mapToRequiredInputType(field)), description = desc)
      })
  }
}

case class FilterObjectTypeBuilder(model: Model, project: Project) {
  def mapToRelationFilterInputField(field: models.Field): List[InputField[_ >: Option[Seq[Any]] <: Option[Any]]] = {
    assert(!field.isScalar)
    val relatedModelInputType = FilterObjectTypeBuilder(field.relatedModel.get, project).filterObjectType

    field.isList match {
      case false =>
        List(InputField(field.name, OptionInputType(relatedModelInputType)))

      case true =>
        FilterArguments
          .getFieldFilters(field)
          .map { filter =>
            InputField(field.name + filter.name, OptionInputType(relatedModelInputType))
          }
    }
  }

  lazy val filterObjectType: InputObjectType[Any] =
    InputObjectType[Any](
      s"${model.name}WhereInput",
      fieldsFn = () => {
        List(
          InputField("AND", OptionInputType(ListInputType(filterObjectType)), description = FilterArguments.ANDFilter.description),
          InputField("OR", OptionInputType(ListInputType(filterObjectType)), description = FilterArguments.ORFilter.description),
          InputField("NOT", OptionInputType(ListInputType(filterObjectType)), description = FilterArguments.NOTFilter.description)
        ) ++ model.scalarFields.filterNot(_.isHidden).flatMap(SchemaBuilderUtils.mapToInputField) ++ model.relationFields.flatMap(mapToRelationFilterInputField)
      }
    )

  // this is just a dummy schema as it is only used by graphiql to validate the subscription input
  lazy val subscriptionFilterObjectType: InputObjectType[Any] =
    InputObjectType[Any](
      s"${model.name}SubscriptionWhereInput",
      () => {
        List(
          InputField("AND", OptionInputType(ListInputType(subscriptionFilterObjectType)), description = FilterArguments.ANDFilter.description),
          InputField("OR", OptionInputType(ListInputType(subscriptionFilterObjectType)), description = FilterArguments.ORFilter.description),
          InputField("NOT", OptionInputType(ListInputType(subscriptionFilterObjectType)), description = FilterArguments.NOTFilter.description),
          InputField(
            "mutation_in",
            OptionInputType(ListInputType(ModelMutationType.Type)),
            description = "The subscription event gets dispatched when it's listed in mutation_in"
          ),
          InputField(
            "updatedFields_contains",
            OptionInputType(StringType),
            description = "The subscription event gets only dispatched when one of the updated fields names is included in this list"
          ),
          InputField(
            "updatedFields_contains_every",
            OptionInputType(ListInputType(StringType)),
            description = "The subscription event gets only dispatched when all of the field names included in this list have been updated"
          ),
          InputField(
            "updatedFields_contains_some",
            OptionInputType(ListInputType(StringType)),
            description = "The subscription event gets only dispatched when some of the field names included in this list have been updated"
          ),
          InputField(
            "node",
            OptionInputType(filterObjectType)
          )
        )
      }
    )

  lazy val internalSubscriptionFilterObjectType: InputObjectType[Any] =
    InputObjectType[Any](
      s"${model.name}SubscriptionWhereInput",
      () => {
        List(
          InputField("AND", OptionInputType(ListInputType(internalSubscriptionFilterObjectType)), description = FilterArguments.ANDFilter.description),
          InputField("OR", OptionInputType(ListInputType(internalSubscriptionFilterObjectType)), description = FilterArguments.ORFilter.description),
          InputField("NOT", OptionInputType(ListInputType(internalSubscriptionFilterObjectType)), description = FilterArguments.NOTFilter.description),
          InputField("boolean",
                     OptionInputType(BooleanType),
                     description = "Placeholder boolean type that will be replaced with the according boolean in the schema"),
          InputField(
            "node",
            OptionInputType(filterObjectType)
          )
        )
      }
    )
}
