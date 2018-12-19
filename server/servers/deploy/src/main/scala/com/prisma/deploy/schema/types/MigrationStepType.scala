package com.prisma.deploy.schema.types

import com.prisma.deploy.schema.SystemUserContext
import com.prisma.shared.models.{Field => _, _}
import com.prisma.shared.models
import sangria.schema
import sangria.schema.{Field, _}
import com.prisma.util.Diff._

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

  case class MigrationStepAndSchema[T <: MigrationStep](step: T, schema: models.Schema, previous: models.Schema)

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
    Field(
      "isEmbedded",
      OptionType(BooleanType),
      resolve = { ctx =>
        val previous = ctx.value.previous.getModelByName_!(ctx.value.step.name).isEmbedded
        val current  = ctx.value.schema.getModelByName_!(ctx.value.step.newName).isEmbedded
        diff(previous, current)
      }
    )
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
    Field(
      "values",
      OptionType(ListType(StringType)),
      resolve = { ctx =>
        val previous = ctx.value.previous.getEnumByName_!(ctx.value.step.name).values
        val current  = ctx.value.schema.getEnumByName_!(ctx.value.step.finalName).values
        diff(previous, current)
      }
    )
  )

  lazy val CreateFieldType = fieldsHelper[CreateField](
    Field("model", StringType, resolve = _.value.step.model),
    Field("name", StringType, resolve = _.value.step.name),
    Field("typeName", StringType, resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.model, ctx.value.step.name).userFriendlyTypeName),
    Field("isRequired", BooleanType, resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.model, ctx.value.step.name).isRequired),
    Field("isList", BooleanType, resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.model, ctx.value.step.name).isList),
    Field("unique", BooleanType, resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.model, ctx.value.step.name).isUnique),
    Field("enum", OptionType(StringType), resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.model, ctx.value.step.name).enum.map(_.name)),
    Field(
      "default",
      OptionType(StringType),
      resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.model, ctx.value.step.name).defaultValue.toString
    ),
    Field(
      "relation",
      OptionType(StringType),
      resolve = { ctx =>
        val (modelName, fieldName) = (ctx.value.step.model, ctx.value.step.name)
        ctx.value.schema.getFieldByName_!(modelName, fieldName).relationOpt.map(_.name)
      }
    )
  )

  lazy val DeleteFieldType = fieldsHelper[DeleteField](
    Field("model", StringType, resolve = _.value.step.model),
    Field("name", StringType, resolve = _.value.step.name)
  )

  def diffField[T](stepAndSchema: MigrationStepAndSchema[UpdateField], fieldFn: models.Field => T): Option[T] = {
    val previous = fieldFn(stepAndSchema.previous.getFieldByName_!(stepAndSchema.step.model, stepAndSchema.step.name))
    val current  = fieldFn(stepAndSchema.schema.getFieldByName_!(stepAndSchema.step.newModel, stepAndSchema.step.finalName))
    diff(previous, current)
  }

  lazy val UpdateFieldType = fieldsHelper[UpdateField](
    Field("model", StringType, resolve = _.value.step.model),
    Field("name", StringType, resolve = _.value.step.name),
    Field("newName", OptionType(StringType), resolve = _.value.step.newName),
    Field(
      "typeName",
      OptionType(StringType),
      resolve = ctx => diffField(ctx.value, _.userFriendlyTypeName)
    ),
    Field(
      "isRequired",
      OptionType(BooleanType),
      resolve = ctx => diffField(ctx.value, _.isRequired)
    ),
    Field(
      "isList",
      OptionType(BooleanType),
      resolve = ctx => diffField(ctx.value, _.isList)
    ),
    Field(
      "unique",
      OptionType(BooleanType),
      resolve = ctx => diffField(ctx.value, _.isUnique)
    ),
    Field(
      "enum",
      OptionType(OptionType(StringType)),
      resolve = ctx => diffField(ctx.value, _.enum.map(_.name))
    ),
    Field(
      "default",
      OptionType(OptionType(StringType)),
      resolve = ctx => diffField(ctx.value, _.defaultValue.map(_.toString))
    ),
    Field(
      "relation",
      OptionType(OptionType(StringType)),
      resolve = ctx => diffField(ctx.value, _.relationOpt.map(_.name))
    )
  )

  lazy val CreateRelationType = fieldsHelper[CreateRelation](
    Field("name", StringType, resolve = _.value.step.name),
    Field("leftModel", StringType, resolve = ctx => ctx.value.schema.getRelationByName_!(ctx.value.step.name).modelAName),
    Field("rightModel", StringType, resolve = ctx => ctx.value.schema.getRelationByName_!(ctx.value.step.name).modelAName)
  )

  lazy val UpdateRelationType = fieldsHelper[UpdateRelation](
    Field("name", StringType, resolve = _.value.step.name),
    Field("newName", OptionType(StringType), resolve = _.value.step.newName)
  )

  lazy val DeleteRelationType = fieldsHelper[DeleteRelation](
    Field("name", StringType, resolve = _.value.step.name)
  )

  lazy val UpdateSecretsType = fieldsHelper[UpdateSecrets](
    Field("message", StringType, resolve = _ => "Secrets have been updated.")
  )

  def fieldsHelper[T <: MigrationStep](fields: schema.Field[SystemUserContext, MigrationStepAndSchema[T]]*)(implicit ct: ClassTag[T]) = {
    def instanceCheck(value: Any, clazz: Class[_], tpe: ObjectType[SystemUserContext, MigrationStepAndSchema[T]]): Boolean = {
      val castedValue = value.asInstanceOf[MigrationStepAndSchema[MigrationStep]]
      ct.runtimeClass.isAssignableFrom(castedValue.step.getClass)
    }
    new ObjectType(
      name = ct.runtimeClass.getSimpleName,
      description = None,
      fieldsFn = () => fields.toList,
      interfaces = interfaces[SystemUserContext, MigrationStepAndSchema[T]](Type).map(_.interfaceType),
      instanceCheck = instanceCheck,
      astDirectives = Vector.empty
    )
  }
}
