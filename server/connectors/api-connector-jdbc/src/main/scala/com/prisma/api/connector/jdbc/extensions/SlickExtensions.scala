package com.prisma.api.connector.jdbc.extensions

import com.prisma.connector.shared.jdbc.SharedSlickExtensions
import com.prisma.gc_values._
import slick.jdbc.PositionedParameters

trait SlickExtensions extends SharedSlickExtensions {
  import SlickExtensionsValueClasses._

  implicit def positionedParameterExtensions(pp: PositionedParameters) = new PositionedParameterExtensions(pp)
}

object SlickExtensionsValueClasses extends JdbcExtensions {

  class PositionedParameterExtensions(val pp: PositionedParameters) extends AnyVal {
    def setGcValue(value: GCValue): Unit = {
      val npos = pp.pos + 1
      pp.ps.setGcValue(npos, value)
      pp.pos = npos
    }
  }
}
