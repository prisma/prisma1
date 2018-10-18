package com.prisma.jwt

import com.prisma.jwt.Algorithm.Algorithm
import com.prisma.jwt.jna.JnaAuth

import scala.util.Try

object Auth {
  def jna(algorithm: Algorithm): Auth = JnaAuth(algorithm)
  def graal(): Auth                   = ???
  def none(): Auth                    = NoAuth
}

trait Auth {
  val algorithm: Algorithm

  def extractToken(header: Option[String]): String = {
    header match {
      case Some(h) => h.stripPrefix("Bearer ")
      case None    => throw AuthFailure("No 'Authorization' header provided.")
    }
  }

  def createToken(secret: String, expirationOffset: Option[Long], grant: Option[JwtGrant] = None): Try[String]
  def verifyToken(token: String, secrets: Vector[String], expectedGrant: Option[JwtGrant] = None): Try[Unit]
}

case class AuthFailure(msg: String) extends Exception(msg)
