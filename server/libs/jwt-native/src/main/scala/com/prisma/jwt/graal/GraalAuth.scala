package com.prisma.jwt.graal

import com.prisma.jwt.Algorithm.Algorithm
import com.prisma.jwt.{Auth, JwtGrant}

import scala.util.Try

case class GraalAuth(algorithm: Algorithm) extends Auth {
  // expirationOffset is the offset in seconds to the current timestamp. None is no expiration at all (todo: edge case: -1).
  def createToken(secret: String, expirationOffset: Option[Long], grant: Option[JwtGrant]): Try[String] = Try {
    ???
  }

  override def verifyToken(token: String, secrets: Vector[String], expectedGrant: Option[JwtGrant]): Try[Unit] = Try {
    ???
  }
}
