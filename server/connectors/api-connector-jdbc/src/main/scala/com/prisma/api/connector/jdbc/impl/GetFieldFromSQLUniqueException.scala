package com.prisma.api.connector.jdbc.impl
import java.sql.SQLIntegrityConstraintViolationException

import com.prisma.shared.models.{Field, Model, Project}
import org.postgresql.util.PSQLException

object GetFieldFromSQLUniqueException {

  def getFieldOption(project: Project, model: Model, e: PSQLException): Option[String] = {
    // see: https://til.hashrocket.com/posts/8f87c65a0a-postgresqls-max-identifier-length-is-63-bytes
    model.scalarFields.filter { field =>
      val constraintNameThatMightBeTooLong = project.dbName + "." + model.dbName + "." + field.dbName + "._UNIQUE"
      val constraintName                   = constraintNameThatMightBeTooLong.substring(0, Math.min(63, constraintNameThatMightBeTooLong.length))
      e.getMessage.contains(constraintName)
    } match {
      case x +: _ => Some("Field name = " + x.name)
      case _      => None
    }
  }

  def getFieldOptionMySql(fieldNames: Vector[String], e: SQLIntegrityConstraintViolationException): Option[String] = {

    fieldNames.filter(x => e.getCause.getMessage.contains("\'" + x + "_")) match {
      case x +: _ => Some("Field name = " + x)
      case _      => None
    }
  }
}
