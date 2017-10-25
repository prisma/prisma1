package cool.graph.shared.authorization

import java.time.Instant

import com.typesafe.config.Config
import cool.graph.DataItem
import cool.graph.shared.models._
import pdi.jwt
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtOptions}
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class JwtUserData[T](projectId: String, userId: String, authData: Option[T], modelName: String = "User")
case class JwtCustomerData(clientId: String)
case class JwtPermanentAuthTokenData(clientId: String, projectId: String, permanentAuthTokenId: String)

object JwtClaimJsonProtocol extends DefaultJsonProtocol {
  implicit val formatClientModel              = jsonFormat(JwtCustomerData, "clientId")
  implicit def formatUserModel[T: JsonFormat] = jsonFormat(JwtUserData.apply[T], "projectId", "userId", "authData", "modelName")
  implicit val formatPermanentAuthTokenModel  = jsonFormat(JwtPermanentAuthTokenData, "clientId", "projectId", "permanentAuthTokenId")
}

trait SharedAuth {
  import JwtClaimJsonProtocol._

  val config: Config
  lazy val jwtSecret: String = config.getString("jwtSecret")
  val expiringSeconds: Int   = 60 * 60 * 24 * 30

  case class Expiration(exp: Long)
  implicit val formatExpiration = jsonFormat(Expiration, "exp")

  def loginUser[T: JsonFormat](project: Project, user: DataItem, authData: Option[T]): Future[String] = {
    val claimPayload = JwtUserData(projectId = project.id, userId = user.id, authData = authData).toJson.compactPrint
    val sessionToken = Jwt.encode(JwtClaim(claimPayload).issuedNow.expiresIn(expiringSeconds), jwtSecret, JwtAlgorithm.HS256)

    Future.successful(sessionToken)
  }

  /**
    * Checks if the given token has an expiration, in which case it checks if the token expired.
    * If the token has no expiration, it is treated as not expired.
    *
    * Note: Assumes JWT secret has already been verified.
    */
  protected def isExpired(sessionToken: String): Boolean = {
    Jwt
      .decodeRaw(sessionToken, JwtOptions(signature = false, expiration = false))
      .map(_.parseJson.convertTo[Expiration])
      .map(_.exp) match {
      case Success(expiration) =>
        (expiration * 1000) < Instant.now().toEpochMilli

      case Failure(e) => {
        // todo: instead of returning false when there is no exp, make sure all tokens have exp
        println("token-had-no-exp-claim")
        false
      }
    }
  }

  protected def parseTokenAsClientData(sessionToken: String): Option[JwtCustomerData] = {
    Jwt
      .decodeRaw(sessionToken, config.getString("jwtSecret"), Seq(JwtAlgorithm.HS256))
      .map(_.parseJson.convertTo[JwtCustomerData])
      .map(Some(_))
      .getOrElse(None)
  }

  def parseTokenAsTemporaryRootToken(token: String): Option[JwtPermanentAuthTokenData] = {
    Jwt
      .decodeRaw(token, config.getString("jwtSecret"), Seq(JwtAlgorithm.HS256))
      .map(_.parseJson.convertTo[JwtPermanentAuthTokenData])
      .map(Some(_))
      .getOrElse(None)
  }

  def isValidTemporaryRootToken(project: Project, token: String): Boolean = {
    parseTokenAsTemporaryRootToken(token) match {
      case Some(rootToken) => !isExpired(token) && rootToken.projectId == project.id
      case None            => false
    }
  }

  def generateRootToken(clientId: String, projectId: String, id: String, expiresInSeconds: Option[Long]): String = {
    val claim = JwtClaim(JwtPermanentAuthTokenData(clientId = clientId, projectId = projectId, permanentAuthTokenId = id).toJson.compactPrint).issuedNow
    val claimToEncode = expiresInSeconds match {
      case Some(expiration) => claim.expiresIn(expiration)
      case None             => claim
    }

    Jwt.encode(
      claimToEncode,
      config.getString("jwtSecret"),
      jwt.JwtAlgorithm.HS256
    )
  }
}
