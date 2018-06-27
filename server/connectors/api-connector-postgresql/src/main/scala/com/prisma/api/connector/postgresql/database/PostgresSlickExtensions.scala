package com.prisma.api.connector.postgresql.database

import com.prisma.api.connector.postgresql.database.JdbcExtensions._
import com.prisma.gc_values._
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}

object PostgresSlickExtensions {

  implicit class PositionedParameterExtensions(val pp: PositionedParameters) extends AnyVal {
    def setGcValue(value: GCValue): Unit = {
      val npos = pp.pos + 1
      pp.ps.setGcValue(npos, value)
      pp.pos = npos
    }
  }

  implicit object SetGcValueParam extends SetParameter[GCValue] {
    override def apply(gcValue: GCValue, pp: PositionedParameters): Unit = {
      val npos = pp.pos + 1
      pp.ps.setGcValue(npos, gcValue)
      pp.pos = npos
    }
  }
}
