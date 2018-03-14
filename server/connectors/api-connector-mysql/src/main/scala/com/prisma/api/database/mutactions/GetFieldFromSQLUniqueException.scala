package com.prisma.api.database.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import com.prisma.api.connector.CoolArgs

object GetFieldFromSQLUniqueException {

  def getFieldOption(values: List[CoolArgs], e: SQLIntegrityConstraintViolationException): Option[String] = {
    val combinedValues: List[(String, Any)] = values.flatMap(_.raw)
    combinedValues.filter(x => e.getCause.getMessage.contains("\'" + x._1 + "_")) match {
      case x if x.nonEmpty => Some("Field name = " + x.head._1)
      case _               => None
    }
  }
}
