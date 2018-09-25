package com.prisma.util.env

import scala.util.Try

object EnvUtils {
  def asInt(name: String): Option[Int] = {
    for {
      value     <- sys.env.get(name)
      converted <- Try(value.toInt).toOption
    } yield converted
  }

  def asBoolean(name: String): Option[Boolean] = {
    for {
      value     <- sys.env.get(name)
      converted <- Try(value.toBoolean).toOption
    } yield converted
  }

}
