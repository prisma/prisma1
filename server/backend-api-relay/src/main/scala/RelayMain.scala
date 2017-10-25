import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.bugsnag.BugSnagger
import cool.graph.client.server.ClientServer
import cool.graph.relay.RelayApiDependencies
import scaldi.Injectable

object RelayMain extends App with Injectable {
  implicit val system       = ActorSystem("sangria-server")
  implicit val materializer = ActorMaterializer()
  implicit val inj          = RelayApiDependencies()
  implicit val bugsnagger   = inject[BugSnagger]

  ServerExecutor(port = 8083, ClientServer("relay")).startBlocking()
}
