package com.prisma.jwt

object JwtGrant {
  def apply(service: String, stage: String, action: String): JwtGrant = new JwtGrant(s"$service/$stage", action)
}

case class JwtGrant(target: String, action: String)
