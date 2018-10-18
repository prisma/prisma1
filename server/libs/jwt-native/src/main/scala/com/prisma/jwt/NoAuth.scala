package com.prisma.jwt
import com.prisma.jwt.Algorithm.Algorithm

import scala.util.Try

object NoAuth extends Auth {
  override lazy val algorithm: Algorithm = Algorithm.HS256

  override def extractToken(header: Option[String]): String = ""

  override def createToken(secret: String, expirationOffset: Option[Long], grant: Option[JwtGrant]): Try[String] = Try {
    "Authentication is not enabled"
  }

  override def verifyToken(token: String, secrets: Vector[String], expectedGrant: Option[JwtGrant]): Try[Unit] = Try {}
}
