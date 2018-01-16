package com.prisma.utils.boolean

object BooleanUtils {
  implicit class BoolToOption(val theBool: Boolean) extends AnyVal {
    def toOption[A](value: => A): Option[A] = if (theBool) Some(value) else None
  }
}
