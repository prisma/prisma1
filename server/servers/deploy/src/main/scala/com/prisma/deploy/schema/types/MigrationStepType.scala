package com.prisma.deploy.schema.types

import com.prisma.deploy.schema.SystemUserContext
import com.prisma.shared.models.{Field => _, _}
import com.prisma.shared.models
import sangria.schema
import sangria.schema.{Field, _}

import scala.reflect.ClassTag

object MigrationStepType {
  lazy val allTypes = List(
    Type,
    CreateModelType,
    DeleteModelType,
    UpdateModelType,
    CreateEnumType,
    DeleteEnumType,
    UpdateEnumType,
    CreateFieldType,
    UpdateFieldType,
    DeleteFieldType,
    CreateRelationType,
    UpdateRelationType,
    DeleteRelationType,
    UpdateSecretsType
  )

  case class MigrationStepAndSchema[T <: MigrationStep](step: T, schema: models.Schema)

  lazy val Type: InterfaceType[SystemUserContext, MigrationStepAndSchema[_]] = InterfaceType(
    "MigrationStep",
    "This is a migration step.",
    fields[SystemUserContext, MigrationStepAndSchema[_]](
      Field("type", StringType, resolve = _.value.step.getClass.getSimpleName)
    )
  )

  lazy val CreateModelType = fieldsHelper[CreateModel](
    Field("name", StringType, resolve = _.value.step.name)
  )

  lazy val DeleteModelType = fieldsHelper[DeleteModel](
    Field("name", StringType, resolve = _.value.step.name)
  )

  lazy val UpdateModelType = fieldsHelper[UpdateModel](
    Field("name", StringType, resolve = _.value.step.name),
    Field("newName", StringType, resolve = _.value.step.newName),
    Field("isEmbedded", OptionType(BooleanType), resolve = ctx => ctx.value.schema.getModelByName_!(ctx.value.step.newName).isEmbedded)
  )

  lazy val CreateEnumType = fieldsHelper[CreateEnum](
    Field("name", StringType, resolve = _.value.step.name),
    Field("values", ListType(StringType), resolve = ctx => ctx.value.schema.getEnumByName_!(ctx.value.step.name).values)
  )

  lazy val DeleteEnumType = fieldsHelper[DeleteEnum](
    Field("name", StringType, resolve = _.value.step.name)
  )

  lazy val UpdateEnumType = fieldsHelper[UpdateEnum](
    Field("name", StringType, resolve = _.value.step.name),
    Field("newName", OptionType(StringType), resolve = _.value.step.newName),
    Field("values", OptionType(ListType(StringType)), resolve = ctx => ctx.value.schema.getEnumByName_!(ctx.value.step.finalName).values)
  )

  lazy val CreateFieldType = fieldsHelper[CreateField](
    Field("model", StringType, resolve = _.value.step.model),
    Field("name", StringType, resolve = _.value.step.name),
    Field("typeName", StringType, resolve = _.value.step.typeName),
    Field("isRequired", BooleanType, resolve = _.value.step.isRequired),
    Field("isList", BooleanType, resolve = _.value.step.isList),
    Field("unique", BooleanType, resolve = _.value.step.isUnique),
    Field("relation", OptionType(StringType), resolve = _.value.step.relation),
    Field("default", OptionType(StringType), resolve = _.value.step.defaultValue),
    Field("enum", OptionType(StringType), resolve = _.value.step.enum)
  )

  lazy val DeleteFieldType = fieldsHelper[DeleteField](
    Field("model", StringType, resolve = _.value.step.model),
    Field("name", StringType, resolve = _.value.step.name)
  )

  lazy val UpdateFieldType = fieldsHelper[UpdateField](
    Field("model", StringType, resolve = _.value.step.model),
    Field("name", StringType, resolve = _.value.step.name),
    Field("newName", OptionType(StringType), resolve = _.value.step.newName),
    Field("typeName", OptionType(StringType), resolve = _.value.step.typeName),
    Field("isRequired", OptionType(BooleanType), resolve = _.value.step.isRequired),
    Field("isList", OptionType(BooleanType), resolve = _.value.step.isList),
    Field("unique", OptionType(BooleanType), resolve = _.value.step.isUnique),
    Field("relation", OptionType(OptionType(StringType)), resolve = _.value.step.relation),
    Field("default", OptionType(OptionType(StringType)), resolve = _.value.step.defaultValue),
    Field("enum", OptionType(OptionType(StringType)), resolve = _.value.step.enum)
  )

  lazy val CreateRelationType = fieldsHelper[CreateRelation](
    Field("name", StringType, resolve = _.value.step.name),
    Field("leftModel", StringType, resolve = _.value.step.modelAName),
    Field("rightModel", StringType, resolve = _.value.step.modelBName)
  )

  lazy val UpdateRelationType = fieldsHelper[UpdateRelation](
    Field("name", StringType, resolve = _.value.step.name),
    Field("newName", OptionType(StringType), resolve = _.value.step.newName),
    Field("modelAId", OptionType(StringType), resolve = _.value.step.modelAId),
    Field("modelBId", OptionType(StringType), resolve = _.value.step.modelBId),
  )

  lazy val DeleteRelationType = fieldsHelper[DeleteRelation](
    Field("name", StringType, resolve = _.value.step.name),
    Field("modelAId", StringType, resolve = _.value.step.modelAName),
    Field("modelBId", StringType, resolve = _.value.step.modelBName)
  )

  lazy val UpdateSecretsType = fieldsHelper[UpdateSecrets](
    Field("message", StringType, resolve = _ => "Secrets have been updated.")
  )

  def fieldsHelper[T <: MigrationStep](fields: schema.Field[SystemUserContext, MigrationStepAndSchema[T]]*)(implicit ct: ClassTag[T]) = {
    ObjectType(
      ct.runtimeClass.getSimpleName,
      "",
      interfaces[SystemUserContext, MigrationStepAndSchema[T]](Type),
      fields.toList
    )
  }
}
