package cool.graph.singleserver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.api.ApiDependenciesImpl
import cool.graph.api.schema.{SchemaBuilder => ApiSchemaBuilder}
import cool.graph.api.server.ApiServer
import cool.graph.deploy.server.DeployServer

object SingleServerMain extends App {
  implicit val system          = ActorSystem("single-server")
  implicit val materializer    = ActorMaterializer()
  implicit val apiDependencies = new ApiDependenciesImpl
  import system.dispatcher

  val port                     = sys.env.getOrElse("PORT", sys.error("PORT env var required but not found.")).toInt
  val singleServerDependencies = SingleServerDependencies()
  val apiSchemaBuilder         = ApiSchemaBuilder()

  Version.check()

  ServerExecutor(
    port = port,
    ApiServer(apiSchemaBuilder),
    DeployServer(singleServerDependencies.schemaBuilder, singleServerDependencies.projectPersistence, singleServerDependencies.client, "system")
  ).startBlocking()
}
