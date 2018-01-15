package cool.graph.singleserver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.api.server.ApiServer
import cool.graph.deploy.server.ClusterServer
import cool.graph.subscriptions.SimpleSubscriptionsServer
import cool.graph.websocket.WebsocketServer
import cool.graph.workers.WorkerServer

object SingleServerMain extends App {
  implicit val system       = ActorSystem("single-server")
  implicit val materializer = ActorMaterializer()

  val port                              = sys.env.getOrElse("PORT", "9000").toInt
  implicit val singleServerDependencies = SingleServerDependencies()

  Version.check()

  ServerExecutor(
    port = port,
    ClusterServer(singleServerDependencies.clusterSchemaBuilder, singleServerDependencies.projectPersistence, "cluster"),
    WebsocketServer(singleServerDependencies),
    ApiServer(singleServerDependencies.apiSchemaBuilder),
    SimpleSubscriptionsServer(),
    WorkerServer(singleServerDependencies)
  ).startBlocking()
}
