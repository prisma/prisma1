package com.prisma.api.schema

import sangria.schema.{Argument, Field, InputObjectType, ObjectType, Schema, SchemaValidationRule}

case class FilterOutEmptyInputTypes(schema: Schema[ApiUserContext, Unit]) {
  private val invalidTypes = schema.allTypes.values.toVector.filter {
    case it: InputObjectType[_] => it.fields.isEmpty
    case _                      => false
  }

  def applyFilter(): Schema[ApiUserContext, Unit] = {
    Schema(
      query = mapObjectType(schema.query),
      mutation = schema.mutation.map(mapObjectType),
      subscription = schema.subscription.map(mapObjectType),
      validationRules = SchemaValidationRule.empty
    )
  }

  private def mapObjectType[A](ot: ObjectType[A, Unit]): ObjectType[A, Unit] = {
    val newFields: List[Field[A, Unit]] = ot.fields.map(mapField).toList
    ot.copy(
      fieldsFn = () => newFields
    )
  }

  private def mapField[A](field: Field[A, _]): Field[A, Unit] = {
    val newArguments = field.arguments.flatMap(mapArgument)
    field.copy(arguments = newArguments).asInstanceOf[Field[A, Unit]]
  }

  private def mapArgument(argument: Argument[_]): Option[Argument[_]] = {
    if (invalidTypes.contains(argument.inputValueType)) {
      None
    } else {
      argument.inputValueType match {
        case iot: InputObjectType[_] =>
          mapInputObjectType(iot).map { iot =>
            argument.copy(argumentType = iot)
          }
        case _ => Some(argument)
      }
    }
  }

  private def mapInputObjectType(it: InputObjectType[_]): Option[InputObjectType[_]] = {
    val invalidFields = it.fields.filter { f =>
      invalidTypes.map(_.name).contains(f.inputValueType.namedType.name)
    }
    val validFields = it.fields.filterNot(f => invalidFields.contains(f))
    if (validFields.isEmpty) {
      None
    } else {
      Some(it.copy(fieldsFn = () => validFields))
    }
  }
}
