package com.prisma.api.connector.postgresql.impl
import com.prisma.shared.models.{Field, Model}
import org.postgresql.util.PSQLException

object GetFieldFromSQLUniqueException {

  def getFieldOption(model: Model, e: PSQLException): Option[String] = {
    model.fields.filter { field =>
      val constraintNameThatMightBeTooLong = model.dbName + "." + field.dbName + "._UNIQUE"
      val constraintName                   = constraintNameThatMightBeTooLong.substring(0, Math.min(30, constraintNameThatMightBeTooLong.length))
      e.getMessage.contains(constraintName)
    } match {
      case x +: _ => Some("Field name = " + x.name)
      case _      => None
    }
  }
}
