package cool.graph.system.schema.types

import sangria.schema._

import cool.graph.shared.models

object SeatStatus {
  val Type = EnumType(
    "SeatStatus",
    values = List(
      EnumValue("JOINED", value = models.SeatStatus.JOINED),
      EnumValue("INVITED_TO_PROJECT", value = models.SeatStatus.INVITED_TO_PROJECT),
      EnumValue("INVITED_TO_GRAPHCOOL", value = models.SeatStatus.INVITED_TO_GRAPHCOOL)
    )
  )
}
