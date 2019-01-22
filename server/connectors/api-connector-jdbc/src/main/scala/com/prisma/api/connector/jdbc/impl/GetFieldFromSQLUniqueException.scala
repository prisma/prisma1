package com.prisma.api.connector.jdbc.impl
import java.sql.{SQLException, SQLIntegrityConstraintViolationException}

import com.prisma.shared.models.{Model, Project}

object GetFieldFromSQLUniqueException {
  def getFieldOption(project: Project, model: Model, e: SQLException): Option[String] = {
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

  def getFieldOptionSQLite(fieldNames: Vector[String], e: SQLException): Option[String] = {
    fieldNames.filter(x => e.getMessage.contains("." + x + ")") && e.getMessage.contains("UNIQUE constraint failed")) match {
      case x +: _ => Some("Field name = " + x)
      case _      => None
    }
  }
}
