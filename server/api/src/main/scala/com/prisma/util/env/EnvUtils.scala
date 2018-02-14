package com.prisma.util.env

import scala.util.Try

object EnvUtils {
  def asInt(name: String): Option[Int] = {
    for {
      value     <- sys.env.get(name)
      converted <- Try(value.toInt).toOption
    } yield converted
  }

}
