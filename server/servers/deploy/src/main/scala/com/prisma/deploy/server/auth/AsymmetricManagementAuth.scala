package com.prisma.deploy.server.auth

import java.time.Instant

import com.prisma.deploy.schema.{InvalidToken, TokenExpired}
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

case class AsymmetricManagementAuth(publicKey: String) extends ManagementAuth {
  import pdi.jwt.{Jwt, JwtAlgorithm, JwtOptions}
  import ManagementAuth._

  override def verify(name: String, stage: String, authHeaderOpt: Option[String]): Try[Unit] = Try {
    authHeaderOpt match {
      case None =>
        throw InvalidToken("'Authorization' header not provided")

      case Some(authHeader) =>
        val jwtOptions = JwtOptions(signature = true, expiration = true)
        val algorithms = Seq(JwtAlgorithm.RS256)
        val decodedToken = Jwt.decodeRaw(
          token = authHeader.stripPrefix("Bearer "),
          key = publicKey,
          algorithms = algorithms,
          options = jwtOptions
        )

        decodedToken match {
          case Failure(exception) =>
            throw InvalidToken(s"Token can't be decoded: ${exception.getMessage}")

          case Success(rawToken) =>
            val token = parseToken(rawToken)
            if ((token.exp * 1000) < Instant.now().toEpochMilli) {
              throw TokenExpired
            }

            token.grants
              .find(verifyGrant(name, stage, _))
              .getOrElse(throw InvalidToken(s"Token contained ${token.grants.length} grants but none satisfied the request."))
        }
    }
  }

  private def verifyGrant(nameToCheck: String, stageToCheck: String, grant: TokenGrant): Boolean = {
    val (grantedName: String, grantedStage: String) = grant.target.split("/").toVector match {
      case Vector(service, stage) => (service, stage)
      case invalid                => throw InvalidToken(s"Contained invalid grant '$invalid'")
    }

    if (grantedName == "" || grantedStage == "") {
      throw InvalidToken(s"Both service and stage must be defined in grant '$grant'")
    }

    validate(nameToCheck, grantedName) && validate(stageToCheck, grantedStage)
  }

  private def validate(toValidate: String, granted: String): Boolean = granted match {
    case "*" => true
    case str => toValidate == str
  }

  private def parseToken(token: String): TokenData = {
    Json.parse(token).asOpt[TokenData] match {
      case None              => throw InvalidToken(s"Failed to parse token data")
      case Some(parsedToken) => parsedToken
    }
  }
}
