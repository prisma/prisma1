package com.prisma.connectors.utils

import java.sql.Driver
import com.prisma.connectors.utils.SupportedDrivers.DBFamily

object SupportedDrivers extends Enumeration {
  type DBFamily = Value
  val MYSQL, POSTGRES, MONGO, SQLITE = Value

  def apply(mapping: (DBFamily, Driver)*): SupportedDrivers = new SupportedDrivers(mapping.toMap)
}

case class DBFamilyNotSupported(family: DBFamily)
    extends Exception(s"DB family ${family.toString.toLowerCase().capitalize} is not supported by this Prisma instance.")

case class SupportedDrivers(mapping: Map[DBFamily, Driver]) {
  def apply(family: DBFamily): Driver       = mapping.getOrElse(family, throw DBFamilyNotSupported(family))
  def get(family: DBFamily): Option[Driver] = mapping.get(family)
}
