package com.prisma.shared.errors

object UserInputErrors {

  case class InvalidName(name: String, entityType: String) extends SystemApiError(InvalidNames.default(name, entityType), 2008)
  object InvalidNames {
    def mustStartUppercase(name: String, entityType: String): String =
      s"'${default(name, entityType)} It must begin with an uppercase letter. It may contain letters and numbers."
    def default(name: String, entityType: String): String = s"'$name' is not a valid name for a$entityType."
  }

}
