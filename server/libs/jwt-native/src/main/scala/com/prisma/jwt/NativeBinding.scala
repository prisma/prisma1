package com.prisma.jwt

import com.prisma.jwt.jna.JnaBinding
import scala.util.Try

object NativeBinding {
  def jna(): NativeBinding   = JnaBinding
  def graal(): NativeBinding = ???
}

trait NativeBinding {
  def createToken(secret: String, expiration: Option[Long]): Try[String]
  def verifyToken(token: String, secrets: Vector[String]): Try[Boolean]
}

case class FailedNativeCallException(msg: String) extends Exception
