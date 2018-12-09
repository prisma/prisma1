package com.prisma.api.schema

import com.prisma.api.schema.CustomScalarTypes.{DateTimeType, JsonType, UUIDType}
import com.prisma.shared.models
import com.prisma.shared.models.{Model, Project, TypeIdentifier}
import sangria.schema._

object SchemaBuilderUtils {
  def mapToOptionalInputType(field: models.ScalarField): InputType[Any] = {
    OptionInputType(mapToRequiredInputType(field))
  }

  def mapToRequiredInputType(field: models.ScalarField): InputType[Any] = {
    assert(field.isScalar)

    val inputType: InputType[Any] = field.typeIdentifier match {
      case TypeIdentifier.String   => StringType
      case TypeIdentifier.Int      => IntType
      case TypeIdentifier.Float    => FloatType
      case TypeIdentifier.Boolean  => BooleanType
      case TypeIdentifier.Cuid     => IDType
      case TypeIdentifier.UUID     => UUIDType
      case TypeIdentifier.DateTime => DateTimeType
      case TypeIdentifier.Json     => JsonType
      case TypeIdentifier.Enum     => mapEnumFieldToInputType(field)
    }

    if (field.isList) {
      ListInputType(inputType)
    } else {
      inputType
    }
  }

  def mapEnumFieldToInputType(field: models.ScalarField): EnumType[Any] = {
    require(field.typeIdentifier == TypeIdentifier.Enum, "This function must be called with Enum fields only!")
    val enum = field.enum.getOrElse(sys.error("A field with TypeIdentifier Enum must always have an enum."))
    EnumType(
      name = enum.name,
      description = None,
      values = enum.values.map(enumValue => EnumValue(enumValue, value = enumValue, description = None)).toList
    )
  }

  def mapToInputField(field: models.ScalarField): List[InputField[_ >: Option[Seq[Any]] <: Option[Any]]] = {
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
  def mapToRelationFilterInputField(field: models.RelationField)                   = relationFilterInputFieldHelper(field)
  def mapToRelationFilterInputFieldForMongo(field: models.RelationField)           = relationFilterInputFieldHelperForMongo(field)
  def mapToRestrictedRelationFilterInputFieldForMongo(field: models.RelationField) = restrictedRelationFilterInputFieldHelperForMongo(field)

  def relationFilterInputFieldHelper(field: models.RelationField): List[InputField[_ >: Option[Seq[Any]] <: Option[Any]]] = {
    assert(!field.isScalar)
    val relatedModelInputType = FilterObjectTypeBuilder(field.relatedModel_!, project).filterObjectType

    (field.isHidden, field.isList) match {
      case (true, _)  => List.empty
      case (_, false) => List(InputField(field.name, OptionInputType(relatedModelInputType)))
      case (_, true)  => FilterArguments.getFieldFilters(field).map(f => InputField(field.name + f.name, OptionInputType(relatedModelInputType)))
    }
  }

  def relationFilterInputFieldHelperForMongo(field: models.RelationField): List[InputField[_ >: Option[Seq[Any]] <: Option[Any]]] = {
    assert(!field.isScalar)
    val relatedModelInputType           = FilterObjectTypeBuilder(field.relatedModel_!, project).filterObjectTypeForMongo
    val restrictedRelatedModelInputType = FilterObjectTypeBuilder(field.relatedModel_!, project).restrictedFilterObjectTypeForMongo

    (field.isHidden, field.isList) match {
      case (true, _)                                     => List.empty
      case (_, false)                                    => List(InputField(field.name, OptionInputType(relatedModelInputType)))
      case (_, true) if !field.relatedModel_!.isEmbedded => List(InputField(field.name + "_some", OptionInputType(relatedModelInputType)))
      case (_, true) if field.relatedModel_!.isEmbedded =>
        List(
          InputField(field.name + "_some", OptionInputType(relatedModelInputType)),
          InputField(field.name + "_every", OptionInputType(restrictedRelatedModelInputType)),
          InputField(field.name + "_none", OptionInputType(restrictedRelatedModelInputType))
        )
    }
  }

  def restrictedRelationFilterInputFieldHelperForMongo(field: models.RelationField): List[InputField[_ >: Option[Seq[Any]] <: Option[Any]]] = {
    assert(!field.isScalar)
    val restrictedRelatedModelInputType = FilterObjectTypeBuilder(field.relatedModel_!, project).restrictedFilterObjectTypeForMongo

    (field.isHidden, field.isList) match {
      case (true, _)                                      => List.empty
      case (_, false) if !field.relatedModel_!.isEmbedded => List.empty
      case (_, false) if field.relatedModel_!.isEmbedded  => List(InputField(field.name, OptionInputType(restrictedRelatedModelInputType)))
      case (_, true) if !field.relatedModel_!.isEmbedded  => List.empty
      case (_, true) if field.relatedModel_!.isEmbedded =>
        FilterArguments.getFieldFilters(field).map(f => InputField(field.name + f.name, OptionInputType(restrictedRelatedModelInputType)))
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

  lazy val scalarFilterObjectType: Option[InputObjectType[Any]] = {
    val fields = model.scalarFields.filterNot(_.isHidden)

    if (fields.nonEmpty) {
      Some(
        InputObjectType[Any](
          s"${model.name}ScalarWhereInput",
          fieldsFn = () => {
            List(
              InputField("AND", OptionInputType(ListInputType(scalarFilterObjectType.get)), description = FilterArguments.ANDFilter.description),
              InputField("OR", OptionInputType(ListInputType(scalarFilterObjectType.get)), description = FilterArguments.ORFilter.description),
              InputField("NOT", OptionInputType(ListInputType(scalarFilterObjectType.get)), description = FilterArguments.NOTFilter.description)
            ) ++ fields.flatMap(SchemaBuilderUtils.mapToInputField)
          }
        ))
    } else {
      None
    }
  }

// this does not Allow NOT/OR and also does not allow _every, _some on non-embedded to many relations
  lazy val filterObjectTypeForMongo: InputObjectType[Any] =
    InputObjectType[Any](
      s"${model.name}WhereInput",
      fieldsFn = () => {
        List(InputField("AND", OptionInputType(ListInputType(filterObjectTypeForMongo)), description = FilterArguments.ANDFilter.description)) ++ model.scalarFields
          .filterNot(_.isHidden)
          .flatMap(SchemaBuilderUtils.mapToInputField) ++ model.relationFields
          .flatMap(mapToRelationFilterInputFieldForMongo)
      }
    )

  // this does not Allow NOT/OR and also only filters on embedded types
  lazy val restrictedFilterObjectTypeForMongo: InputObjectType[Any] =
    InputObjectType[Any](
      s"${model.name}RestrictedWhereInput",
      fieldsFn = () => {
        List(InputField("AND", OptionInputType(ListInputType(restrictedFilterObjectTypeForMongo)), description = FilterArguments.ANDFilter.description)) ++ model.scalarFields
          .filterNot(_.isHidden)
          .flatMap(SchemaBuilderUtils.mapToInputField) ++ model.relationFields
          .filter(_.relatedModel_!.isEmbedded)
          .flatMap(mapToRestrictedRelationFilterInputFieldForMongo)
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

  // this is just a dummy schema as it is only used by graphiql to validate the subscription input
  lazy val subscriptionFilterObjectTypeForMongo: InputObjectType[Any] =
    InputObjectType[Any](
      s"${model.name}SubscriptionWhereInput",
      () => {
        List(
          InputField("AND", OptionInputType(ListInputType(subscriptionFilterObjectTypeForMongo)), description = FilterArguments.ANDFilter.description),
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
            OptionInputType(filterObjectTypeForMongo)
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

  lazy val internalSubscriptionFilterObjectTypeForMongo: InputObjectType[Any] =
    InputObjectType[Any](
      s"${model.name}SubscriptionWhereInput",
      () => {
        List(
          InputField("AND", OptionInputType(ListInputType(internalSubscriptionFilterObjectTypeForMongo)), description = FilterArguments.ANDFilter.description),
          InputField("boolean",
                     OptionInputType(BooleanType),
                     description = "Placeholder boolean type that will be replaced with the according boolean in the schema"),
          InputField(
            "node",
            OptionInputType(filterObjectTypeForMongo)
          )
        )
      }
    )
}
