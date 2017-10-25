package cool.graph.system.authorization

import com.github.t3hnar.bcrypt._
import com.typesafe.config.Config
import cool.graph.Types.Id
import cool.graph.shared.authorization.JwtCustomerData
import cool.graph.shared.errors.UserInputErrors.DuplicateEmailFromMultipleProviders
import cool.graph.system.database.tables.Client
import cool.graph.system.database.tables.Tables._
import cool.graph.utils.future.FutureUtils._
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

import scaldi._
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SystemAuth()(implicit inj: Injector) extends Injectable {

  import cool.graph.shared.authorization.JwtClaimJsonProtocol._

  val internalDatabase = inject[DatabaseDef](identified by "internal-db")
  val config           = inject[Config](identified by "config")
  val masterToken      = inject[Option[String]](identified by "master-token")

  type SessionToken = String

  val expiringSeconds = 60 * 60 * 24 * 30

  def login(email: String, password: String): Future[Option[(SessionToken, Id)]] = {
    internalDatabase
      .run(Clients.filter(_.email === email).take(1).result.headOption)
      .map {
        case Some(client) if password.isBcrypted(client.password) =>
          val sessionToken = Jwt.encode(JwtClaim(JwtCustomerData(client.id).toJson.compactPrint).issuedNow, config.getString("jwtSecret"), JwtAlgorithm.HS256)
          Some((sessionToken, client.id))

        case _ =>
          None
      }
  }

  def trustedLogin(email: String, secret: String): Future[Option[(SessionToken, Id)]] = {
    if (secret != config.getString("systemApiSecret")) {
      return Future.successful(None)
    }

    internalDatabase
      .run(Clients.filter(_.email === email).take(1).result.headOption)
      .map {
        case Some(client) =>
          val sessionToken = Jwt.encode(JwtClaim(JwtCustomerData(client.id).toJson.compactPrint).issuedNow, config.getString("jwtSecret"), JwtAlgorithm.HS256)
          Some((sessionToken, client.id))

        case _ =>
          None
      }
  }

  def loginByAuth0IdToken(idToken: String): Future[Option[(SessionToken, Id)]] = {
    // Check if we run in a local env with a master token and if not, use the usual flow
    masterToken match {
      case Some(token) =>
        if (idToken == token) {
          masterTokenFlow(idToken)
        } else {
          throw new Exception("Invalid token for local env")
        }

      case None =>
        loginByAuth0IdTokenFlow(idToken)
    }
  }

  def masterTokenFlow(idToken: String): Future[Option[(SessionToken, Id)]] = {
    internalDatabase
      .run(Clients.result)
      .flatMap { (customers: Seq[Client]) =>
        if (customers.nonEmpty) {
          generateSessionToken(customers.head.id).map(sessionToken => Some((sessionToken, customers.head.id)))
        } else {
          throw new Exception("Inconsistent local state: Master user was not initialized correctly.")
        }
      }
  }

  /**
    * Rules:
    * existing auth0id - sign in
    * no auth0Id, existing email that matches - sign in and add auth0Id
    * no auth0Id, no email - create user and add auth0Id
    * no auth0Id, existing email for other user - reject
    */
  def loginByAuth0IdTokenFlow(idToken: String): Future[Option[(SessionToken, Id)]] = {
    val idTokenData = parseAuth0IdToken(idToken)

    if (idTokenData.isEmpty) {
      return Future.successful(None)
    }

    val isAuth0IdentityProviderEmail = idTokenData.get.sub.split("\\|").head == "auth0"

    internalDatabase
      .run(
        Clients
          .filter(c => c.email === idTokenData.get.email || c.auth0Id === idTokenData.get.sub)
          .result)
      .flatMap(customers => {
        customers.length match {
          case 0 => {
            // create user and add auth0Id
            Future.successful(None)
          }
          case 1 => {
            (customers.head.auth0Id, customers.head.email) match {
              case (None, email) => {
                // sign in and add auth0Id
                generateSessionToken(customers.head.id).andThenFuture(
                  handleSuccess = res =>
                    internalDatabase.run((for {
                      c <- Clients if c.id === customers.head.id
                    } yield (c.auth0Id, c.isAuth0IdentityProviderEmail))
                      .update((Some(idTokenData.get.sub), isAuth0IdentityProviderEmail))),
                  handleFailure = e => Future.successful(())
                ) map (sessionToken => Some((sessionToken, customers.head.id)))
              }
              case (Some(auth0Id), email) if auth0Id == idTokenData.get.sub => {
                // sign in
                generateSessionToken(customers.head.id).map(sessionToken => Some((sessionToken, customers.head.id)))

              }
              case (Some(auth0Id), email)
                  // note: the isEmail check is disabled until we fix the Auth0 account linking issue
                  if (auth0Id != idTokenData.get.sub) /*&& !isAuth0IdentityProviderEmail*/ => {
                // Auth0 returns wrong id first time for linked accounts.
                // Let's just go ahead and match on email only as long as it is provided by a social provider
                // that has already verified the email
                generateSessionToken(customers.head.id).map(sessionToken => Some((sessionToken, customers.head.id)))

              }
              case (Some(auth0Id), email) if auth0Id != idTokenData.get.sub => {
                // reject
                throw DuplicateEmailFromMultipleProviders(email)
              }
            }
          }
          case 2 => {
            // we fucked up
            throw new Exception("Two different users exist with the idToken and email")
          }
        }
      })
  }

  def loginByResetPasswordToken(resetPasswordToken: String): Future[Option[(SessionToken, Id)]] = {
    internalDatabase
      .run(
        Clients
          .filter(_.resetPasswordToken === resetPasswordToken)
          .take(1)
          .result
          .headOption)
      .flatMap {
        case Some(client) => generateSessionToken(client.id).map(sessionToken => Some((sessionToken, client.id)))
        case _            => Future.successful(None)
      }
  }

  def generateSessionToken(clientId: String): Future[String] = Future.successful {
    Jwt.encode(JwtClaim(JwtCustomerData(clientId).toJson.compactPrint).issuedNow, config.getString("jwtSecret"), JwtAlgorithm.HS256)
  }

  def generateSessionTokenWithExpiration(clientId: String): String = {
    Jwt.encode(JwtClaim(JwtCustomerData(clientId).toJson.compactPrint).issuedNow.expiresIn(expiringSeconds), config.getString("jwtSecret"), JwtAlgorithm.HS256)
  }

  def parseAuth0IdToken(idToken: String): Option[Auth0IdTokenData] = {
    implicit val a = Auth0IdTokenDataJsonProtocol.formatAuth0IdTokenData

    val decodedSecret = new String(
      new sun.misc.BASE64Decoder()
        .decodeBuffer(config.getString("auth0jwtSecret")))

    Jwt
      .decodeRaw(idToken, decodedSecret, Seq(JwtAlgorithm.HS256))
      .map(_.parseJson.convertTo[Auth0IdTokenData])
      .map(Some(_))
      .getOrElse(None)
  }

  def parseSessionToken(sessionToken: SessionToken): Option[String] = {
    SystemAuth2().clientId(sessionToken)
  }
}

case class Auth0IdTokenData(sub: String, email: String, name: String, exp: Option[Int], user_metadata: Option[UserMetaData])
case class UserMetaData(name: String)

object Auth0IdTokenDataJsonProtocol extends DefaultJsonProtocol {
  implicit val formatUserMetaData = jsonFormat(UserMetaData, "name")
  implicit val formatAuth0IdTokenData =
    jsonFormat(Auth0IdTokenData, "sub", "email", "name", "exp", "user_metadata")
}
