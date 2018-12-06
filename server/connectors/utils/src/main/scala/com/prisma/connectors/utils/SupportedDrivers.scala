package com.prisma.connectors.utils

import java.sql.Driver
import com.prisma.connectors.utils.SupportedDrivers.DBFamiliy

object SupportedDrivers extends Enumeration {
  type DBFamiliy = Value
  val MYSQL, POSTGRES, MONGO = Value

  def apply(mapping: (DBFamiliy, Driver)*): SupportedDrivers = new SupportedDrivers(mapping.toMap)
}

case class DBFamiliyNotSupported(familiy: DBFamiliy)
    extends Exception(s"DB family ${familiy.toString.toLowerCase().capitalize} is not supported by this Prisma instance.")

case class SupportedDrivers(mapping: Map[DBFamiliy, Driver]) {
  def apply(familiy: DBFamiliy): Driver       = mapping.getOrElse(familiy, throw DBFamiliyNotSupported(familiy))
  def get(familiy: DBFamiliy): Option[Driver] = mapping.get(familiy)
}
