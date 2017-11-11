package cool.graph.system.authorization

import com.typesafe.config.Config
import cool.graph.shared.authorization.{JwtCustomerData, JwtPermanentAuthTokenData, JwtUserData, SharedAuth}
import cool.graph.shared.models.Project
import pdi.jwt
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import scaldi.{Injectable, Injector}

case class SystemAuth2()(implicit inj: Injector) extends SharedAuth with Injectable {
  import spray.json._
  import cool.graph.shared.authorization.JwtClaimJsonProtocol._

  val config = inject[Config](identified by "config")

  // todo: should we include optional authData as string?
  def generateNodeToken(project: Project, nodeId: String, modelName: String, expirationInSeconds: Option[Int]): String = {
    val claimPayload        = JwtUserData[String](projectId = project.id, userId = nodeId, authData = None, modelName = modelName).toJson.compactPrint
    val finalExpiresIn: Int = expirationInSeconds.getOrElse(expiringSeconds)
    val token               = Jwt.encode(JwtClaim(claimPayload).issuedNow.expiresIn(finalExpiresIn), jwtSecret, JwtAlgorithm.HS256)

    token
  }

  def clientId(sessionToken: String): Option[String] = {
    if (isExpired(sessionToken)) {
      None
    } else {
      parseTokenAsClientData(sessionToken).map(_.clientId)
    }
  }

  def generatePlatformTokenWithExpiration(clientId: String): String = {
    Jwt.encode(JwtClaim(JwtCustomerData(clientId).toJson.compactPrint).issuedNow.expiresIn(expiringSeconds), config.getString("jwtSecret"), JwtAlgorithm.HS256)
  }
}
