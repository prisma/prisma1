package com.prisma.utils.boolean

import com.prisma.utils.boolean.BooleanUtils.BoolToOption

trait BooleanUtils {
  implicit def boolToOptionExtension(theBool: Boolean): BoolToOption = BoolToOption(theBool)
}

object BooleanUtils {
  implicit class BoolToOption(val theBool: Boolean) extends AnyVal {
    def toOption[A](value: => A): Option[A] = if (theBool) Some(value) else None
  }
}
