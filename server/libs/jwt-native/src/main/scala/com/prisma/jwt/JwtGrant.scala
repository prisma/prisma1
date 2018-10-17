package com.prisma.jwt

case class JwtGrant(target: String, grant: String) extends Grant {
  override def getTarget: String = target
  override def getGrant: String  = grant
}
