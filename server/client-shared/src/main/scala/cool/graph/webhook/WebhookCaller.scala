package cool.graph.webhook

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import cool.graph.cuid.Cuid
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait WebhookCaller {
  def call(url: String, payload: String): Future[Boolean]
}

class WebhookCallerMock extends WebhookCaller {
  private val _calls = scala.collection.parallel.mutable.ParTrieMap[String, (String, String)]()

  def calls = _calls.values.toList

  var nextCallShouldFail = false

  def clearCalls = _calls.clear

  override def call(url: String, payload: String): Future[Boolean] = {
    _calls.put(Cuid.createCuid(), (url, payload))

    Future.successful(!nextCallShouldFail)
  }
}

class WebhookCallerImplementation(implicit inj: Injector) extends WebhookCaller with Injectable {

  override def call(url: String, payload: String): Future[Boolean] = {

    implicit val system       = inject[ActorSystem](identified by "actorSystem")
    implicit val materializer = inject[ActorMaterializer](identified by "actorMaterializer")

    println("calling " + url)

    Http()
      .singleRequest(HttpRequest(uri = url, method = HttpMethods.POST, entity = HttpEntity(contentType = ContentTypes.`application/json`, string = payload)))
      .map(x => {
        x.status.isSuccess()
      })
  }
}
