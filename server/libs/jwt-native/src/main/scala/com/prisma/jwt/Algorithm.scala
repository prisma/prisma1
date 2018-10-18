package com.prisma.jwt

object Algorithm extends Enumeration {
  type Algorithm = Value

  // todo RS* not supported at the moment
  //  val HS256, RS256 = Value
  val HS256 = Value
}
