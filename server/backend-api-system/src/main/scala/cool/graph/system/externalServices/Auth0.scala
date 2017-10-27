package cool.graph.system.externalServices

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Auth0ApiUpdateValues(email: Option[String])

trait Auth0Api {
  def updateClient(auth0Id: String, values: Auth0ApiUpdateValues): Future[Boolean]
}

class Auth0ApiMock extends Auth0Api {
  var lastUpdate: Option[(String, Auth0ApiUpdateValues)] = None

  override def updateClient(auth0Id: String, values: Auth0ApiUpdateValues): Future[Boolean] = {

    lastUpdate = Some((auth0Id, values))

    Future.successful(true)
  }
}

class Auth0ApiImplementation(implicit inj: Injector) extends Auth0Api with Injectable {

  override def updateClient(auth0Id: String, values: Auth0ApiUpdateValues): Future[Boolean] = {

    implicit val system = inject[ActorSystem](identified by "actorSystem")
    implicit val materializer =
      inject[ActorMaterializer](identified by "actorMaterializer")

    val config        = inject[Config](identified by "config")
    val auth0Domain   = config.getString("auth0Domain")
    val auth0ApiToken = config.getString("auth0ApiToken")

    Http()
      .singleRequest(
        HttpRequest(
          uri = s"https://${auth0Domain}/api/v2/users/${auth0Id}",
          method = HttpMethods.PATCH,
          entity = HttpEntity(contentType = ContentTypes.`application/json`, string = s"""{"email":"${values.email.get}"}""")
        ).addCredentials(OAuth2BearerToken(auth0ApiToken)))
      .map(_.status.intValue match {
        case 200 => true
        case _   => false
      })
  }
}
