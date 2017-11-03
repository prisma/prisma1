package cool.graph.system.externalServices

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, StreamTcpException}
import cool.graph.akkautil.http.{FailedRequestError, SimpleHttpClient}
import scaldi.{Injectable, Injector}
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AlgoliaKeyChecker {
  def verifyAlgoliaCredentialValidity(appId: String, apiKey: String): Future[Boolean]
}

class AlgoliaKeyCheckerMock() extends AlgoliaKeyChecker {
  var returnValue: Boolean = true
  override def verifyAlgoliaCredentialValidity(appId: String, apiKey: String): Future[Boolean] = {
    Future.successful(returnValue)
  }

  def setReturnValueToFalse() = {
    returnValue = false
  }
}

object AlgoliaKeyChecker extends DefaultJsonProtocol {
  implicit val AlgoliaFormat = jsonFormat(AlgoliaResponse, "acl")
  case class AlgoliaResponse(acl: String)
}

class AlgoliaKeyCheckerImplementation(implicit inj: Injector) extends AlgoliaKeyChecker with Injectable {
  implicit val system       = inject[ActorSystem](identified by "actorSystem")
  implicit val materializer = inject[ActorMaterializer](identified by "actorMaterializer")

  val httpClient = SimpleHttpClient()

  // For documentation see: https://www.algolia.com/doc/rest-api/search#get-the-rights-of-a-global-api-key
  override def verifyAlgoliaCredentialValidity(appId: String, apiKey: String): Future[Boolean] = {
    if (appId.isEmpty || apiKey.isEmpty) {
      Future.successful(false)
    } else {
      val headers = Seq("X-Algolia-Application-Id" -> appId, "X-Algolia-API-Key" -> apiKey)

      httpClient
        .get(s"https://$appId.algolia.net/1/keys/$apiKey", headers)
        .map { response =>
          response.body.contains("addObject") && response.body.contains("deleteObject")
        }
        .recover {
          // https://[INVALID].algolia.net/1/keys/[VALID] times out, so we simply report a timeout as a wrong appId
          case _: StreamTcpException => false
          case _: FailedRequestError => false
        }
    }
  }
}
