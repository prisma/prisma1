package com.prisma.jwt

import com.prisma.jwt.Algorithm.Algorithm
import com.prisma.jwt.jna.JnaAuth

import scala.util.Try

object Auth {
  def jna(algorithm: Algorithm): Auth = JnaAuth(algorithm)
  def graal(): Auth                   = ???
}

trait Auth {
  val algorithm: Algorithm

  def createToken(secret: String, expirationOffset: Option[Int]): Try[String]
  def verifyToken(token: String, secrets: Vector[String]): Try[Unit]
  def verifyTokenGrant(expectedGrant: JwtGrant, token: String, secrets: Vector[String]): Try[Unit]
}

case class AuthFailure(msg: String) extends Exception(msg)
