package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._

trait SharedDirectiveValidation {
  def validateStringValue(value: sangria.ast.Value): Option[String] = {
    value match {
      case v: sangria.ast.StringValue => None
      case _                          => Some("This argument must be a String.")
    }
  }

  def validateEnumValue(argument: String)(validValues: Vector[String])(value: sangria.ast.Value): Option[String] = {
    if (validValues.contains(value.asString)) {
      None
    } else {
      Some(s"Valid values for the argument `$argument` are: ${validValues.mkString(",")}.")
    }
  }
}
