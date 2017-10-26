package cool.graph.client

import cool.graph.client.database.{FilterArgument, FilterArguments}
import cool.graph.client.schema.ModelMutationType
import cool.graph.shared.models
import cool.graph.shared.models.{Model, Project, TypeIdentifier}
import cool.graph.shared.schema.CustomScalarTypes.{DateTimeType, JsonType, PasswordType}
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
      case TypeIdentifier.Password  => PasswordType
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
      enum.name,
      field.description,
      enum.values.map(enumValue => EnumValue(enumValue, value = enumValue, description = None)).toList
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

class FilterObjectTypeBuilder(model: Model, project: Project) {
  def mapToRelationFilterInputField(field: models.Field): List[InputField[_ >: Option[Seq[Any]] <: Option[Any]]] = {
    assert(!field.isScalar)
    val relatedModelInputType = new FilterObjectTypeBuilder(field.relatedModel(project).get, project).filterObjectType

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
      s"${model.name}Filter",
      fieldsFn = () => {
        List(
          InputField("AND", OptionInputType(ListInputType(filterObjectType)), description = FilterArguments.ANDFilter.description),
          InputField("OR", OptionInputType(ListInputType(filterObjectType)), description = FilterArguments.ORFilter.description)
        ) ++ model.fields
          .filter(_.isScalar)
          .flatMap(SchemaBuilderUtils.mapToInputField) ++ model.fields
          .filter(!_.isScalar)
          .flatMap(mapToRelationFilterInputField)
      }
    )

  // this is just a dummy schema as it is only used by graphiql to validate the subscription input
  lazy val subscriptionFilterObjectType: InputObjectType[Any] =
    InputObjectType[Any](
      s"${model.name}SubscriptionFilter",
      () => {
        List(
          InputField("AND", OptionInputType(ListInputType(subscriptionFilterObjectType)), description = FilterArguments.ANDFilter.description),
          InputField("OR", OptionInputType(ListInputType(subscriptionFilterObjectType)), description = FilterArguments.ORFilter.description),
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
            OptionInputType(
              InputObjectType[Any](
                s"${model.name}SubscriptionFilterNode",
                () => {
                  model.fields
                    .filter(_.isScalar)
                    .flatMap(SchemaBuilderUtils.mapToInputField) ++ model.fields
                    .filter(!_.isScalar)
                    .flatMap(mapToRelationFilterInputField)
                }
              )
            )
          )
        )
      }
    )

  lazy val internalSubscriptionFilterObjectType: InputObjectType[Any] =
    InputObjectType[Any](
      s"${model.name}SubscriptionFilter",
      () => {
        List(
          InputField("AND", OptionInputType(ListInputType(internalSubscriptionFilterObjectType)), description = FilterArguments.ANDFilter.description),
          InputField("OR", OptionInputType(ListInputType(internalSubscriptionFilterObjectType)), description = FilterArguments.ORFilter.description),
          InputField("boolean",
                     OptionInputType(BooleanType),
                     description = "Placeholder boolean type that will be replaced with the according boolean in the schema"),
          InputField(
            "node",
            OptionInputType(
              InputObjectType[Any](
                s"${model.name}SubscriptionFilterNode",
                () => {
                  model.fields
                    .filter(_.isScalar)
                    .flatMap(SchemaBuilderUtils.mapToInputField) ++ model.fields
                    .filter(!_.isScalar)
                    .flatMap(mapToRelationFilterInputField)
                }
              )
            )
          )
        )
      }
    )
}
