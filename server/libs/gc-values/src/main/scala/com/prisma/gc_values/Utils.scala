package com.prisma.gc_values

import java.util.UUID

import scala.util.Try

object UUIDUtil {
  def parse(s: String): Try[UuidGCValue] = Try { UuidGCValue(UUID.fromString(s)) }
}
