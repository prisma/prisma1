import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.client.server.ClientServer
import cool.graph.relay.RelayInjector

object RelayMain extends App {
  implicit val system       = ActorSystem("sangria-server")
  implicit val materializer = ActorMaterializer()
  implicit val injector     = RelayInjector()
  implicit val bugsnagger   = injector.bugSnagger

  ServerExecutor(port = 8083, ClientServer("relay")).startBlocking()
}
