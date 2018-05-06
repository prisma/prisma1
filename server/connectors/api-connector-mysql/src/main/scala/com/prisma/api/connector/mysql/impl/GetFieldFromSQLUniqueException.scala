package com.prisma.api.connector.mysql.impl

import java.sql.SQLIntegrityConstraintViolationException

object GetFieldFromSQLUniqueException {

  def getFieldOption(fieldNames: Vector[String], e: SQLIntegrityConstraintViolationException): Option[String] = {

    fieldNames.filter(x => e.getCause.getMessage.contains("\'" + x + "_")) match {
      case x +: _ => Some("Field name = " + x)
      case _      => None
    }
  }
}
