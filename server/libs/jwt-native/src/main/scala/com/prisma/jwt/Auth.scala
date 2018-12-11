package com.prisma.jwt

import com.prisma.jwt.Algorithm.Algorithm
import scala.util.Try

trait Auth {
  val algorithm: Algorithm

  // No expiration value for the native code
  val NO_EXP: Int = -1

  def extractToken(header: Option[String]): String = {
    header match {
      case Some(h) => normalizeToken(h)
      case None    => throw AuthFailure("No 'Authorization' header provided.")
    }
  }

  def normalizeToken(token: String) = token.stripPrefix("Bearer ")
  def createToken(secret: String, expirationOffset: Option[Long], grant: Option[JwtGrant] = None): Try[String]
  def verifyToken(token: String, secrets: Vector[String], expectedGrant: Option[JwtGrant] = None): Try[Unit]
}

case class AuthFailure(msg: String) extends Exception(msg)
