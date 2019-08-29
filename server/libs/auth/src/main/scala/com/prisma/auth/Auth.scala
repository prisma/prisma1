package com.prisma.auth

import java.time.Instant

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtOptions}

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
  private val jwtOptions      = JwtOptions(signature = true, expiration = true)
  private val algorithm       = JwtAlgorithm.HS256
  private val algorithms      = Seq(algorithm)
  private val secondsOfOneDay = 86400

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

  override def createToken(secrets: Vector[String]) = createToken(secrets, secondsOfOneDay)

  def createToken(secrets: Vector[String], expirationInSeconds: Long) = {
    secrets.headOption match {
      case Some(secret) =>
        val nowInSeconds = Instant.now().toEpochMilli / 1000
        val claim        = JwtClaim(expiration = Some(nowInSeconds + expirationInSeconds), notBefore = Some(nowInSeconds))
        Jwt.encode(claim, secret, algorithm)
      case None =>
        ""
    }
  }

  def verify(secrets: Vector[String], authHeader: String): AuthResult = {
    val isValid = secrets.exists { secret =>
      val claims = Jwt.decodeRaw(token = authHeader.stripPrefix("Bearer "), key = secret, algorithms = algorithms, options = jwtOptions)
      claims.isSuccess
    }
    if (isValid) AuthSuccess else AuthFailure
  }
}
