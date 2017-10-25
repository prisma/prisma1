package cool.graph.graphql

import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import cool.graph.akkautil.SingleThreadedActorSystem
import play.api.libs.json.{JsPath, JsValue, Json, Reads}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait GraphQlClient {
  def sendQuery(query: String): Future[GraphQlResponse]
}

object GraphQlClient {
  private implicit lazy val actorSystem       = SingleThreadedActorSystem("graphql-client")
  private implicit lazy val actorMaterializer = ActorMaterializer()(actorSystem)
  private implicit lazy val akkaHttp          = Http()(actorSystem)

  def apply(uri: String, headers: Map[String, String] = Map.empty): GraphQlClient = {
    GraphQlClientImpl(uri, headers, akkaHttp)
  }
}

case class GraphQlResponse(status: Int, body: String) {
  def bodyAs[T](path: String)(implicit reads: Reads[T]): Try[T] = {
    def jsPathForElements(pathElements: Seq[String], current: JsPath = JsPath()): JsPath = {
      if (pathElements.isEmpty) {
        current
      } else {
        jsPathForElements(pathElements.tail, current \ pathElements.head)
      }
    }
    val jsPath      = jsPathForElements(path.split('.'))
    val actualReads = jsPath.read(reads)
    jsonBody.map(_.as(actualReads))
  }

  val is2xx: Boolean = status >= 200 && status <= 299
  val is200: Boolean = status == 200
  val is404: Boolean = status == 404

  def isSuccess: Boolean = deserializedBody match {
    case Success(x) => x.errors.isEmpty && is200
    case Failure(e) => false
  }

  def isFailure: Boolean       = !isSuccess
  def firstError: GraphQlError = deserializedBody.get.errors.head

  private lazy val deserializedBody: Try[GraphQlResponseJson] = {
    for {
      body     <- jsonBody
      response <- Try { body.as(JsonReaders.graphqlResponseReads) }
    } yield response
  }

  lazy val jsonBody: Try[JsValue] = Try(Json.parse(body))
}

case class GraphQlResponseJson(data: JsValue, errors: Seq[GraphQlError])
case class GraphQlError(message: String, code: Int)

object JsonReaders {
  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit lazy val graphqlErrorReads = Json.reads[GraphQlError]
  implicit lazy val graphqlResponseReads = (
    (JsPath \ "data").read[JsValue] and
      (JsPath \ "errors").readNullable[Seq[GraphQlError]].map(_.getOrElse(Seq.empty))
  )(GraphQlResponseJson.apply _)
}
