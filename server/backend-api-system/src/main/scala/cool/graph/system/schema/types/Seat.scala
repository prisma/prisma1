package cool.graph.system.schema.types

import cool.graph.shared.models
import cool.graph.system.SystemUserContext
import sangria.schema._

object Seat {
  lazy val Type: ObjectType[SystemUserContext, models.Seat] = ObjectType(
    "Seat",
    "This is a seat",
    interfaces[SystemUserContext, models.Seat](nodeInterface),
    idField[SystemUserContext, models.Seat] ::
      fields[SystemUserContext, models.Seat](
      Field("isOwner", BooleanType, resolve = _.value.isOwner),
      Field("email", StringType, resolve = _.value.email),
      Field("name", OptionType(StringType), resolve = _.value.name),
      Field("status", SeatStatusType, resolve = _.value.status)
    )
  )
}
