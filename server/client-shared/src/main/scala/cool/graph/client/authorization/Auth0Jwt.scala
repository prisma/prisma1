package cool.graph.client.authorization

import cool.graph.shared.models.{AuthProviderAuth0, IntegrationName, Project}
import pdi.jwt.{Jwt, JwtAlgorithm, JwtOptions}
import spray.json._

import scala.util.{Success, Try}

object Auth0Jwt {
  import Auth0AuthJsonProtocol._

  def parseTokenAsAuth0AuthData(project: Project, idToken: String): Option[JwtAuth0AuthData] = {
    for {
      authProvider <- project.authProviders.find(_.name == IntegrationName.AuthProviderAuth0)
      meta         <- authProvider.metaInformation
      clientSecret = meta.asInstanceOf[AuthProviderAuth0].clientSecret
      decoded      <- decode(secret = clientSecret, idToken = idToken).toOption
    } yield {
      val idToken = decoded.parseJson.convertTo[IdToken]
      JwtAuth0AuthData(auth0UserId = idToken.sub)
    }
  }

  // Auth0 has two versions of client secrets: https://auth0.com/forum/t/client-secret-stored-without-base64-encoding/4338/22
  // issued before Dec 2016: Base64
  // issued after Dec 2016: UTF8
  private def decode(secret: String, idToken: String): Try[String] = {
    val jwtOptions = JwtOptions(signature = true, expiration = false)
    val algorithms = Seq(JwtAlgorithm.HS256)
    val fromUtf8   = Jwt.decodeRaw(token = idToken, key = secret, algorithms = algorithms, options = jwtOptions)

    fromUtf8 match {
      case Success(jwt) =>
        Success(jwt)
      case _ =>
        val base64DecodedSecret = new String(new sun.misc.BASE64Decoder().decodeBuffer(secret))
        Jwt.decodeRaw(token = idToken, key = base64DecodedSecret, algorithms = algorithms, options = jwtOptions)
    }

  }
}

case class JwtAuth0AuthData(auth0UserId: String)
case class IdToken(iss: String, sub: String, aud: String, exp: Int, iat: Int)

object Auth0AuthJsonProtocol extends DefaultJsonProtocol {
  implicit val authDataFormat: RootJsonFormat[JwtAuth0AuthData] = jsonFormat1(JwtAuth0AuthData)
  implicit val idTokenFormat: RootJsonFormat[IdToken]           = jsonFormat5(IdToken)
}
