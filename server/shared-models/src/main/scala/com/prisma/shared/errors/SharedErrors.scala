package com.prisma.shared.errors

object SharedErrors {
  sealed trait SharedError extends Exception {
    def message: String

    override def getMessage: String = message
  }
  abstract class AbstractSharedError(val message: String) extends SharedError

  case class InvalidModel(reason: String) extends AbstractSharedError(reason)
}
