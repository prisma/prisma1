package cool.graph.websockets

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.bugsnag.BugSnaggerImpl
import cool.graph.subscriptions.websockets.services.WebsocketCloudServives

object WebsocketMain extends App {
  implicit val system       = ActorSystem("graphql-subscriptions")
  implicit val materializer = ActorMaterializer()
  implicit val bugsnag      = BugSnaggerImpl(sys.env("BUGSNAG_API_KEY"))

  val services = WebsocketCloudServives()

  ServerExecutor(port = 8085, WebsocketServer(services)).startBlocking()
}
