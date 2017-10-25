package cool.graph.client.authorization

import com.typesafe.config.Config
import cool.graph.DataItem
import cool.graph.client.database.ProjectDataresolver
import cool.graph.shared.authorization.{JwtCustomerData, JwtPermanentAuthTokenData, JwtUserData, SharedAuth}
import cool.graph.shared.models._
import cool.graph.utils.future.FutureUtils._
import pdi.jwt.{Jwt, JwtAlgorithm}
import scaldi.{Injectable, Injector}
import spray.json.JsonFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait ClientAuth {
  def authenticateRequest(sessionToken: String, project: Project): Future[AuthenticatedRequest]

  def loginUser[T: JsonFormat](project: Project, user: DataItem, authData: Option[T]): Future[String]
}

case class ClientAuthImpl()(implicit inj: Injector) extends ClientAuth with SharedAuth with Injectable {
  import cool.graph.shared.authorization.JwtClaimJsonProtocol._
  import spray.json._

  val config = inject[Config](identified by "config")

  /**
    * Input: userToken, clientToken, permanentAuthToken
    * Returns a userId if:
    * - userToken is not expired and belongs to a user in the project
    * - clientToken is not expired and belongs to a collaborator of the project
    * - permanentAuthToken belongs to the project
    */
  def authenticateRequest(sessionToken: String, project: Project): Future[AuthenticatedRequest] = {
    tokenFromPermanentRootTokens(sessionToken, project).toFutureTry
      .flatMap {
        case Success(authedReq) => Future.successful(authedReq)
        case Failure(_)         => ensureTokenIsValid(sessionToken).flatMap(_ => tryAuthenticateToken(sessionToken, project))
      }
  }

  private def tokenFromPermanentRootTokens(token: String, project: Project): Future[AuthenticatedRequest] = {
    project.rootTokens.find(_.token == token) match {
      case Some(RootToken(id, _, _, _)) => Future.successful(AuthenticatedRootToken(id, token))
      case None                         => Future.failed(new Exception(s"Token is not a PAT: '$token'"))
    }
  }

  private def ensureTokenIsValid(token: String): Future[Unit] = {
    if (isExpired(token)) {
      Future.failed(new Exception(s"Token has expired '$token'"))
    } else {
      Future.successful(())
    }
  }

  private def tryAuthenticateToken(token: String, project: Project): Future[AuthenticatedRequest] = {
    val tmpRootTokenData = parseTokenAsTemporaryRootToken(token)
    val clientData       = parseTokenAsClientData(token)
    val userData         = parseTokenAsJwtUserData(token)
    val auth0Data        = parseTokenAsAuth0AuthData(token, project)

    (tmpRootTokenData, clientData, userData, auth0Data) match {
      case (Some(JwtPermanentAuthTokenData(_, projectId, tokenId)), _, _, _) if projectId == project.id =>
        tokenFromTemporaryRootToken(tokenId, token)

      case (_, Some(JwtCustomerData(jwtClientId)), _, _) =>
        tokenFromCollaborators(jwtClientId, token, project)

      case (_, _, Some(JwtUserData(projectId, userId, _, typeName)), _) if projectId == project.id =>
        tokenFromUsers(userId, typeName, token, project)

      case (_, _, _, Some(JwtAuth0AuthData(auth0UserId))) =>
        tokenFromAuth0(auth0UserId, token, project)

      case _ =>
        Future.failed(new Exception(s"Couldn't parse token '$token'"))
    }
  }

  def parseTokenAsJwtUserData(sessionToken: String): Option[JwtUserData[Unit]] = {
    Jwt
      .decodeRaw(sessionToken, config.getString("jwtSecret"), Seq(JwtAlgorithm.HS256))
      .map(_.parseJson.convertTo[JwtUserData[Unit]])
      .map(Some(_))
      .getOrElse(None)
  }

  private def parseTokenAsAuth0AuthData(sessionToken: String, project: Project): Option[JwtAuth0AuthData] = {
    Auth0Jwt.parseTokenAsAuth0AuthData(project, sessionToken)
  }

  private def tokenFromCollaborators(clientId: String, token: String, project: Project): Future[AuthenticatedRequest] = {
    if (customerIsCollaborator(clientId, project)) {
      Future.successful(AuthenticatedCustomer(clientId, token))
    } else {
      throw new Exception(s"The provided token is valid, but the customer is not a collaborator: '$token'")
    }
  }

  private def customerIsCollaborator(customerId: String, project: Project) = project.seats.exists(_.clientId.contains(customerId))

  private def tokenFromUsers(userId: String, typeName: String, token: String, project: Project): Future[AuthenticatedRequest] = {
    userFromDb(userId, typeName, project).map { _ =>
      AuthenticatedUser(userId, typeName, token)
    }
  }

  private def userFromDb(userId: String, typeName: String, project: Project): Future[DataItem] = {
    val dataResolver = new ProjectDataresolver(project = project, requestContext = None)

    for {
      user <- dataResolver.resolveByUnique(
               Model("someId", typeName, None, isSystem = true, List()),
               "id",
               userId
             )
    } yield {
      user.getOrElse(throw new Exception(s"The provided token is valid, but the user no longer exists: '$userId'"))
    }
  }

  private def tokenFromAuth0(auth0UserId: String, token: String, project: Project): Future[AuthenticatedRequest] = {
    getUserIdForAuth0User(auth0UserId, project).map {
      case Some(userId) => AuthenticatedUser(userId, "User", token)
      case None         => throw new Exception(s"The provided Auth0 token is valid, but the user no longer exists: '$token'")
    }
  }

  private def tokenFromTemporaryRootToken(id: String, token: String): Future[AuthenticatedRequest] = Future.successful(AuthenticatedRootToken(id, token))

  private def getUserIdForAuth0User(auth0Id: String, project: Project): Future[Option[String]] = {
    val dataResolver = new ProjectDataresolver(project = project, requestContext = None)
    dataResolver.resolveByUnique(dataResolver.project.getModelByName_!("User"), ManagedFields.auth0UserId.defaultName, auth0Id).map(_.map(_.id))
  }
}
