package com.prisma.jwt

object Algorithm extends Enumeration {
  type Algorithm = Value

  val HS256, RS256 = Value
}
