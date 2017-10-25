package cool.graph.system.schema.types

import cool.graph.system.database.finder
import sangria.schema._

object HistogramPeriod {
  lazy val Type = EnumType(
    "HistogramPeriod",
    values = List(
      EnumValue("MONTH", value = finder.HistogramPeriod.MONTH),
      EnumValue("WEEK", value = finder.HistogramPeriod.WEEK),
      EnumValue("DAY", value = finder.HistogramPeriod.DAY),
      EnumValue("HOUR", value = finder.HistogramPeriod.HOUR),
      EnumValue("HALF_HOUR", value = finder.HistogramPeriod.HALF_HOUR)
    )
  )
}
