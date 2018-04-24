package com.prisma.deploy.server.auth

import play.api.libs.json.Json

import scala.util.Try

object ManagementAuth {
  implicit val tokenGrantReads = Json.reads[TokenGrant]
  implicit val tokenDataReads  = Json.reads[TokenData]
}

trait ManagementAuth {
  def verify(name: String, stage: String, authHeaderOpt: Option[String]): Try[Unit]
}

case class TokenData(grants: Vector[TokenGrant], exp: Long)
case class TokenGrant(target: String, action: String)
