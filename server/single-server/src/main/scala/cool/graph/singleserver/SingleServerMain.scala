package cool.graph.singleserver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.api.server.ApiServer
import cool.graph.bugsnag.BugSnaggerImpl
import cool.graph.deploy.server.ClusterServer
import cool.graph.subscriptions.SimpleSubscriptionsServer
import cool.graph.websocket.WebsocketServer

object SingleServerMain extends App {
  implicit val system       = ActorSystem("single-server")
  implicit val materializer = ActorMaterializer()

  val port                              = sys.env.getOrElse("PORT", "9000").toInt
  implicit val singleServerDependencies = SingleServerDependencies()
  implicit val bugsnagger               = BugSnaggerImpl(sys.env.getOrElse("BUGSNAG_API_KEY", ""))

  Version.check()

  ServerExecutor(
    port = port,
    ClusterServer(singleServerDependencies.clusterSchemaBuilder, singleServerDependencies.projectPersistence, "cluster"),
    WebsocketServer(singleServerDependencies.websocketServices),
    ApiServer(singleServerDependencies.apiSchemaBuilder),
    SimpleSubscriptionsServer()
  ).startBlocking()
}
