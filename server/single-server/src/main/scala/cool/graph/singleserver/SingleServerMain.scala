package cool.graph.singleserver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.api.ApiDependenciesImpl
import cool.graph.api.server.ApiServer
import cool.graph.deploy.server.{ClusterServer, SchemaServer}

object SingleServerMain extends App {
  implicit val system          = ActorSystem("single-server")
  implicit val materializer    = ActorMaterializer()
  implicit val apiDependencies = new ApiDependenciesImpl

  val port                     = sys.env.getOrElse("PORT", "9000").toInt
  val singleServerDependencies = SingleServerDependencies()

  Version.check()

  ServerExecutor(
    port = port,
    ClusterServer(singleServerDependencies.clusterSchemaBuilder, "cluster"),
    SchemaServer(singleServerDependencies.projectPersistence, "cluster"),
    ApiServer(singleServerDependencies.apiSchemaBuilder),
    SchemaServer(singleServerDependencies.projectPersistence)
  ).startBlocking()
}
