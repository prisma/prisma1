package cool.graph.deploy
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.deploy.server.DeployServer

object DeployMain extends App {
  implicit val system       = ActorSystem("deploy-main")
  implicit val materializer = ActorMaterializer()

  val dependencies = DeployDependenciesImpl()
  val server       = DeployServer(dependencies.deploySchemaBuilder, dependencies.projectPersistence, "system")
  ServerExecutor(8081, server).startBlocking()
}
