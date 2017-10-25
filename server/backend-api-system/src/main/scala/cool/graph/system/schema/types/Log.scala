package cool.graph.system.schema.types

import cool.graph.shared.models
import cool.graph.shared.schema.CustomScalarTypes
import cool.graph.system.SystemUserContext
import sangria.schema.{Field, _}

object Log {

  lazy val Type: ObjectType[SystemUserContext, models.Log] = ObjectType[SystemUserContext, models.Log](
    "Log",
    "A log is a log is a log",
    interfaces[SystemUserContext, models.Log](nodeInterface),
    idField[SystemUserContext, models.Log] ::
      fields[SystemUserContext, models.Log](
      Field("requestId", OptionType(StringType), resolve = ctx => ctx.value.requestId),
      Field("duration", IntType, resolve = ctx => ctx.value.duration),
      Field("status", LogStatusType, resolve = ctx => ctx.value.status),
      Field("timestamp", CustomScalarTypes.DateTimeType, resolve = ctx => ctx.value.timestamp),
      Field("message", StringType, resolve = ctx => ctx.value.message)
    )
  )
}
