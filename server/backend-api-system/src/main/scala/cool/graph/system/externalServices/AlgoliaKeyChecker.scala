package cool.graph.system.externalServices

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.{ActorMaterializer, StreamTcpException}
import scaldi.{Injectable, Injector}
import spray.json.DefaultJsonProtocol

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

class AlgoliaKeyCheckerImplementation(implicit inj: Injector) extends AlgoliaKeyChecker with Injectable {
  implicit val system = inject[ActorSystem](identified by "actorSystem")
  implicit val materializer =
    inject[ActorMaterializer](identified by "actorMaterializer")

  case class AlgoliaResponse(acl: String)
  object AlgoliaJsonProtocol extends DefaultJsonProtocol {
    implicit val AlgoliaFormat = jsonFormat(AlgoliaResponse, "acl")
  }

  // For documentation see: https://www.algolia.com/doc/rest-api/search#get-the-rights-of-a-global-api-key
  override def verifyAlgoliaCredentialValidity(appId: String, apiKey: String): Future[Boolean] = {

    if (appId.isEmpty || apiKey.isEmpty) {
      Future.successful(false)
    } else {

      val algoliaUri          = Uri(s"https://${appId}.algolia.net/1/keys/${apiKey}")
      val algoliaAppIdHeader  = RawHeader("X-Algolia-Application-Id", appId)
      val algoliaApiKeyHeader = RawHeader("X-Algolia-API-Key", apiKey)
      val algoliaHeaders      = List(algoliaAppIdHeader, algoliaApiKeyHeader)

      val request  = HttpRequest(method = HttpMethods.GET, uri = algoliaUri, headers = algoliaHeaders)
      val response = Http().singleRequest(request)
      response.map {
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          val responseString = entity.toString()
          val requiredPermissionsPresent = responseString.contains("addObject") && responseString
            .contains("deleteObject")

          requiredPermissionsPresent

        case _ =>
          false
      } recover {
        // https://[INVALID].algolia.net/1/keys/[VALID] times out, so we simply report a timeout as a wrong appId
        case _: StreamTcpException => false
      }
    }
  }
}
