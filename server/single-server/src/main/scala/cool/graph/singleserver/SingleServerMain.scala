package cool.graph.singleserver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.api.ApiDependenciesImpl
import cool.graph.api.server.ApiServer
import cool.graph.deploy.server.ClusterServer
import cool.graph.subscriptions.{SimpleSubscriptionsServer, SubscriptionDependenciesImpl}
import cool.graph.websocket.WebsocketServer
import cool.graph.websocket.services.WebsocketDevDependencies

object SingleServerMain extends App {
  implicit val system       = ActorSystem("single-server")
  implicit val materializer = ActorMaterializer()

  val port                              = sys.env.getOrElse("PORT", "9000").toInt
  val subscriptionDependencies          = SubscriptionDependenciesImpl()
  implicit val singleServerDependencies = SingleServerDependencies(subscriptionDependencies.sssEventsPubSub)
  val websocketDependencies             = WebsocketDevDependencies(subscriptionDependencies.requestsQueuePublisher, subscriptionDependencies.responsePubSubscriber)
  import subscriptionDependencies.bugSnagger

  Version.check()

  ServerExecutor(
    port = port,
    ClusterServer(singleServerDependencies.clusterSchemaBuilder, singleServerDependencies.projectPersistence, "cluster"),
    WebsocketServer(websocketDependencies),
    ApiServer(singleServerDependencies.apiSchemaBuilder),
    SimpleSubscriptionsServer()(subscriptionDependencies, system, materializer)
  ).startBlocking()
}
