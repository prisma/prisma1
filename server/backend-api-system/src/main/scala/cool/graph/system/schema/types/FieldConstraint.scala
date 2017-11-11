package cool.graph.system.schema.types

import cool.graph.shared.models
import cool.graph.system.SystemUserContext
import sangria.schema.{Field, _}

object FieldConstraint {

  lazy val Type: InterfaceType[SystemUserContext, models.FieldConstraint] = InterfaceType(
    "FieldConstraint",
    "This is a FieldConstraint",
    fields[SystemUserContext, models.FieldConstraint](
      Field("id", IDType, resolve = _.value.id),
      Field("constraintType", FieldConstraintTypeType.Type, resolve = _.value.constraintType),
      Field("fieldId", IDType, resolve = _.value.fieldId)
    )
  )
}

object StringConstraint {

  lazy val Type: ObjectType[SystemUserContext, models.StringConstraint] =
    ObjectType[SystemUserContext, models.StringConstraint](
      "StringConstraint",
      "This is a StringConstraint",
      interfaces[SystemUserContext, models.StringConstraint](nodeInterface, FieldConstraint.Type),
      fields[SystemUserContext, models.StringConstraint](
        Field("equalsString", OptionType(StringType), resolve = _.value.equalsString),
        Field("oneOfString", OptionType(ListType(StringType)), resolve = _.value.oneOfString),
        Field("minLength", OptionType(IntType), resolve = _.value.minLength),
        Field("maxLength", OptionType(IntType), resolve = _.value.maxLength),
        Field("startsWith", OptionType(StringType), resolve = _.value.startsWith),
        Field("endsWith", OptionType(StringType), resolve = _.value.endsWith),
        Field("includes", OptionType(StringType), resolve = _.value.includes),
        Field("regex", OptionType(StringType), resolve = _.value.regex)
      )
    )
}

object NumberConstraint {

  lazy val Type: ObjectType[SystemUserContext, models.NumberConstraint] =
    ObjectType[SystemUserContext, models.NumberConstraint](
      "NumberConstraint",
      "This is a NumberConstraint",
      interfaces[SystemUserContext, models.NumberConstraint](nodeInterface, FieldConstraint.Type),
      fields[SystemUserContext, models.NumberConstraint](
        Field("equalsNumber", OptionType(FloatType), resolve = _.value.equalsNumber),
        Field("oneOfNumber", OptionType(ListType(FloatType)), resolve = _.value.oneOfNumber),
        Field("min", OptionType(FloatType), resolve = _.value.min),
        Field("max", OptionType(FloatType), resolve = _.value.max),
        Field("exclusiveMin", OptionType(FloatType), resolve = _.value.exclusiveMin),
        Field("exclusiveMax", OptionType(FloatType), resolve = _.value.exclusiveMax),
        Field("multipleOf", OptionType(FloatType), resolve = _.value.multipleOf)
      )
    )
}

object BooleanConstraint {

  lazy val Type: ObjectType[SystemUserContext, models.BooleanConstraint] =
    ObjectType[SystemUserContext, models.BooleanConstraint](
      "BooleanConstraint",
      "This is a BooleanConstraint",
      interfaces[SystemUserContext, models.BooleanConstraint](nodeInterface, FieldConstraint.Type),
      fields[SystemUserContext, models.BooleanConstraint](
        Field("equalsBoolean", OptionType(BooleanType), resolve = _.value.equalsBoolean)
      )
    )
}

object ListConstraint {

  lazy val Type: ObjectType[SystemUserContext, models.ListConstraint] =
    ObjectType[SystemUserContext, models.ListConstraint](
      "ListConstraint",
      "This is a ListConstraint",
      interfaces[SystemUserContext, models.ListConstraint](nodeInterface, FieldConstraint.Type),
      fields[SystemUserContext, models.ListConstraint](
        Field("uniqueItems", OptionType(BooleanType), resolve = _.value.uniqueItems),
        Field("minItems", OptionType(IntType), resolve = _.value.minItems),
        Field("maxItems", OptionType(IntType), resolve = _.value.maxItems)
      )
    )
}
