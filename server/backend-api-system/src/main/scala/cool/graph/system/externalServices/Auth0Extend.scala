package cool.graph.system.externalServices

import akka.http.scaladsl.model._
import com.twitter.io.Buf
import cool.graph.shared.models.Client
import cool.graph.system.authorization.SystemAuth2
import scaldi.{Injectable, Injector}

import scala.concurrent.Future

case class Auth0FunctionData(url: String, auth0Id: String)

trait Auth0Extend {
  def createAuth0Function(client: Client, code: String): Future[Auth0FunctionData]
}

class Auth0ExtendMock extends Auth0Extend {
  var lastCode: Option[String] = None
  var shouldFail: Boolean      = false

  override def createAuth0Function(client: Client, code: String): Future[Auth0FunctionData] = {
    lastCode = Some(code)

    if (shouldFail) {
      sys.error("some error deploying Auth0 Extend function")
    }

    Future.successful(Auth0FunctionData("http://some.url", auth0Id = "some-id"))
  }
}

class Auth0ExtendImplementation(implicit inj: Injector) extends Auth0Extend with Injectable {

  override def createAuth0Function(client: Client, code: String): Future[Auth0FunctionData] = {

    import com.twitter.conversions.time._
    import com.twitter.finagle
    import cool.graph.twitterFutures.TwitterFutureImplicits._
    import spray.json.DefaultJsonProtocol._
    import spray.json._

    import scala.concurrent.ExecutionContext.Implicits.global

    // todo: inject this
    val extendEndpoint = "https://d0b5iw4041.execute-api.eu-west-1.amazonaws.com/prod/create/"
    val clientToken    = SystemAuth2().generatePlatformTokenWithExpiration(clientId = client.id)

    def toDest(s: String) = s"${Uri(s).authority.host}:${Uri(s).effectivePort}"
    val extendService =
      finagle.Http.client.withTls(Uri(extendEndpoint).authority.host.address()).withRequestTimeout(15.seconds).newService(toDest(extendEndpoint))

    val body = Map("code" -> code, "authToken" -> clientToken).toJson.prettyPrint

    val request = com.twitter.finagle.http
      .RequestBuilder()
      .url(extendEndpoint)
      .buildPost(Buf.Utf8(body))
    request.setContentTypeJson()

    for {
      json <- extendService(request)
               .map(res => {
                 res.getContentString().parseJson
               })
               .asScala
    } yield {
      Auth0FunctionData(
        url = json.asJsObject.fields("url").convertTo[String],
        auth0Id = json.asJsObject.fields("fn").convertTo[String]
      )
    }
  }
}
