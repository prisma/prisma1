import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.client.schema.simple.SimpleInjector
import cool.graph.client.server.ClientServer

object SimpleMain extends App {
  implicit val system       = ActorSystem("sangria-server")
  implicit val materializer = ActorMaterializer()
  implicit val injector     = SimpleInjector()
  implicit val bugsnagger   = injector.bugsnagger

  ServerExecutor(port = 8080, ClientServer("simple")).startBlocking()
}
