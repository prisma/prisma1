package cool.graph.deploy.schema.types

import cool.graph.deploy.schema.SystemUserContext
import cool.graph.shared.models._
import sangria.schema
import sangria.schema.{Field, _}

import scala.reflect.ClassTag

object MigrationStepType {
  lazy val Type: InterfaceType[SystemUserContext, MigrationStep] = InterfaceType(
    "MigrationStep",
    "This is a migration step.",
    fields[SystemUserContext, MigrationStep](
      Field("type", StringType, resolve = ctx => { ctx.value.getClass.getSimpleName })
    )
  )

  lazy val CreateModelType = fieldsHelper[CreateModel](
    List(
      Field("name", StringType, resolve = ctx => { ctx.value.name })
    )
  )

  lazy val DeleteModelType = fieldsHelper[DeleteModel](
    List(
      Field("name", StringType, resolve = ctx => { ctx.value.name })
    )
  )

  lazy val UpdateModelType = fieldsHelper[UpdateModel](
    List(
      Field("name", StringType, resolve = ctx => { ctx.value.name }),
      Field("newName", StringType, resolve = ctx => { ctx.value.newName })
    )
  )

  def fieldsHelper[T <: MigrationStep](fields: List[schema.Field[SystemUserContext, T]])(implicit ct: ClassTag[T]) = {
    ObjectType(
      "",
      "",
      interfaces[SystemUserContext, T](Type),
      fields
    )
  }
}
