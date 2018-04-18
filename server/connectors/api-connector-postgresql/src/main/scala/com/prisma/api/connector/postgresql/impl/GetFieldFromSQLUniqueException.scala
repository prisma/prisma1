package com.prisma.api.connector.postgresql.impl
import org.postgresql.util.PSQLException

object GetFieldFromSQLUniqueException {

  def getFieldOption(fieldNames: Vector[String], e: PSQLException): Option[String] = {

    fieldNames.filter(x => e.getMessage.contains(x + "._UNIQUE")) match {
      case x +: _ => Some("Field name = " + x)
      case _      => None
    }
  }
}
