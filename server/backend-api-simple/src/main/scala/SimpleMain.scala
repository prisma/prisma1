import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.bugsnag.BugSnagger
import cool.graph.client.schema.simple.SimpleApiDependencies
import cool.graph.client.server.ClientServer
import scaldi.Injectable

object SimpleMain extends App with Injectable {
  implicit val system       = ActorSystem("sangria-server")
  implicit val materializer = ActorMaterializer()
  implicit val inj          = SimpleApiDependencies()
  implicit val bugsnagger   = inject[BugSnagger]

  ServerExecutor(port = 8080, ClientServer("simple")).startBlocking()
  
}
