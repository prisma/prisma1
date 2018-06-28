package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.jdbc.database.JdbcExtensions._
import com.prisma.gc_values._
import slick.jdbc.PositionedParameters

object SlickExtensions {

  implicit class PositionedParameterExtensions(val pp: PositionedParameters) extends AnyVal {
    def setGcValue(value: GCValue): Unit = {
      val npos = pp.pos + 1
      pp.ps.setGcValue(npos, value)
      pp.pos = npos
    }
  }
}
