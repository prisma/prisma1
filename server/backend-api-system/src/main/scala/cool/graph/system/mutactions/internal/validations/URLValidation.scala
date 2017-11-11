package cool.graph.system.mutactions.internal.validations

import java.net.{MalformedURLException, URL}

import cool.graph.shared.errors.{UserAPIErrors, UserInputErrors}

object URLValidation {
  def getAndValidateURL(functionName: String, input: Option[String]): String = {
    input match {
      case None =>
        throw UserAPIErrors.InvalidValue("Url")
      case Some(url) =>
        try {
          val trimmedString = url.trim
          new URL(trimmedString)
          trimmedString
        } catch {
          case _: MalformedURLException => throw UserInputErrors.FunctionHasInvalidUrl(functionName, url)
        }
    }
  }
}
