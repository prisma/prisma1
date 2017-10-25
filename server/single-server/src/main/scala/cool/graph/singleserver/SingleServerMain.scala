package cool.graph.singleserver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.bugsnag.BugSnagger
import cool.graph.client.server.ClientServer
import cool.graph.schemamanager.SchemaManagerServer
import cool.graph.subscriptions.SimpleSubscriptionsServer
import cool.graph.subscriptions.websockets.services.WebsocketServices
import cool.graph.system.SystemServer
import cool.graph.websockets.WebsocketServer
import cool.graph.worker.WorkerServer
import cool.graph.worker.services.WorkerServices
import scaldi.Injectable

object SingleServerMain extends App with Injectable {
  implicit val system       = ActorSystem("single-server")
  implicit val materializer = ActorMaterializer()
  implicit val inj          = SingleServerDependencies()
  implicit val bugsnagger   = inject[BugSnagger]

  val workerServices    = inject[WorkerServices](identified by "worker-services")
  val websocketServices = inject[WebsocketServices](identified by "websocket-services")
  val port              = sys.env.getOrElse("PORT", sys.error("PORT env var required but not found.")).toInt

  Version.check()

  ServerExecutor(
    port = port,
    SystemServer(inj.schemaBuilder, "system"),
    SchemaManagerServer("schema-manager"),
    ClientServer("simple"),
    ClientServer("relay"),
    WebsocketServer(websocketServices, "subscriptions"),
    SimpleSubscriptionsServer(),
    WorkerServer(workerServices)
  ).startBlocking()
}
