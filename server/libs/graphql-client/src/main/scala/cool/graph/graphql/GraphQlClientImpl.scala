package cool.graph.graphql

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import play.api.libs.json.Json

import scala.concurrent.Future

case class GraphQlClientImpl(uri: String, headers: Map[String, String], akkaHttp: HttpExt)(
    implicit system: ActorSystem,
    materializer: ActorMaterializer
) extends GraphQlClient {
  import system.dispatcher

  def sendQuery(query: String): Future[GraphQlResponse] = {
    val body   = Json.obj("query" -> query)
    val entity = HttpEntity(ContentTypes.`application/json`, body.toString)
    val akkaHeaders = headers
      .flatMap {
        case (key, value) =>
          HttpHeader.parse(key, value) match {
            case Ok(header, _) => Some(header)
            case _             => None
          }
      }
      .to[collection.immutable.Seq]

    val akkaRequest = HttpRequest(
      uri = uri,
      method = HttpMethods.POST,
      entity = entity,
      headers = akkaHeaders
    )

    akkaHttp.singleRequest(akkaRequest).flatMap(convertResponse)
  }

  private def convertResponse(akkaResponse: HttpResponse): Future[GraphQlResponse] = {
    Unmarshal(akkaResponse).to[String].map { bodyString =>
      GraphQlResponse(akkaResponse.status.intValue, bodyString)
    }
  }
}
