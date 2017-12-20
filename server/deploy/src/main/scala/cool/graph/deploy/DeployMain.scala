package cool.graph.deploy
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.deploy.server.{ClusterServer, SchemaServer}

object DeployMain extends App {
  implicit val system       = ActorSystem("deploy-main")
  implicit val materializer = ActorMaterializer()

  val dependencies  = DeployDependenciesImpl()
  val clusterServer = ClusterServer(dependencies.clusterSchemaBuilder, "cluster")
  val schemaServer  = SchemaServer(dependencies.projectPersistence, "cluster")
  ServerExecutor(8081, clusterServer, schemaServer).startBlocking()
}
