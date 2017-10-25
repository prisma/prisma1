import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.bugsnag.BugSnagger
import cool.graph.schemamanager.{SchemaManagerDependencies, SchemaManagerServer}
import scaldi.Injectable

object SchemaManagerMain extends App with Injectable {
  implicit val system       = ActorSystem("sangria-server")
  implicit val materializer = ActorMaterializer()
  implicit val inj          = SchemaManagerDependencies()
  implicit val bugSnagger   = inject[BugSnagger]

  ServerExecutor(port = 8087, SchemaManagerServer("schema-manager")).startBlocking()
}
