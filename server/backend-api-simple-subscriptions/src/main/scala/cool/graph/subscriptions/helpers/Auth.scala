package cool.graph.subscriptions.helpers

import cool.graph.client.authorization.{ClientAuth, ClientAuthImpl}
import cool.graph.shared.models.{AuthenticatedRequest, Project}
import scaldi.{Injectable, Injector}
import cool.graph.utils.future.FutureUtils._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Auth extends Injectable {
  def getAuthContext(project: Project, authHeader: Option[String])(implicit inj: Injector, ec: ExecutionContext): Future[Option[AuthenticatedRequest]] = {
    val clientAuth = inject[ClientAuth]
    val token = authHeader.flatMap {
      case str if str.startsWith("Bearer ") => Some(str.stripPrefix("Bearer "))
      case _                                => None
    }

    token match {
      case None => Future.successful(None)
      case Some(sessionToken) =>
        clientAuth
          .authenticateRequest(sessionToken, project)
          .toFutureTry
          .flatMap {
            case Success(authedReq) => Future.successful(Some(authedReq))
            case Failure(_)         => Future.successful(None)
          }
    }
  }
}
