package com.prisma.auth

import pdi.jwt.{Jwt, JwtAlgorithm, JwtOptions}

trait Auth {
  def verify(secrets: Vector[String], authHeader: Option[String]): AuthResult

  def createToken(secrets: Vector[String]): String
}

sealed trait AuthResult {
  def isSuccess: Boolean
}
object AuthSuccess extends AuthResult {
  override def isSuccess = true
}
object AuthFailure extends AuthResult {
  override def isSuccess = false
}

object AuthImpl extends Auth {
  private val jwtOptions = JwtOptions(signature = true, expiration = false)
  private val algorithms = Seq(JwtAlgorithm.HS256)

  override def verify(secrets: Vector[String], authHeader: Option[String]): AuthResult = {
    if (secrets.isEmpty) {
      AuthSuccess
    } else {
      authHeader match {
        case None       => AuthFailure
        case Some(auth) => verify(secrets, auth)
      }
    }
  }

  override def createToken(secrets: Vector[String]) = {
    secrets.headOption match {
      case Some(secret) => Jwt.encode("irrelevant-claim", secret, algorithms.head)
      case None         => ""
    }
  }

  private def verify(secrets: Vector[String], authHeader: String): AuthResult = {
    val isValid = secrets.exists { secret =>
      val claims = Jwt.decodeRaw(token = authHeader.stripPrefix("Bearer "), key = secret, algorithms = algorithms, options = jwtOptions)
      // todo: also verify claims in accordance with https://github.com/graphcool/framework/issues/1365
      claims.isSuccess
    }
    if (isValid) AuthSuccess else AuthFailure
  }
}
