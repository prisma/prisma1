package cool.graph.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import cool.graph.api.database.Databases
import cool.graph.api.project.{ProjectFetcher, ProjectFetcherImpl}
import cool.graph.api.schema.SchemaBuilder
import cool.graph.utils.await.AwaitUtils

import scala.concurrent.Await

trait ApiDependencies extends AwaitUtils {
  val config: Config = ConfigFactory.load()

  val system: ActorSystem
  val materializer: ActorMaterializer
  val projectFetcher: ProjectFetcher
  val apiSchemaBuilder: SchemaBuilder
  val databases: Databases

  def destroy = {
    println("ApiDependencies [DESTROY]")
    databases.master.shutdown.await()
    databases.readOnly.shutdown.await()
    materializer.shutdown()
    system.terminate().await()
  }
}

case class ApiDependenciesImpl(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ApiDependencies {
  val databases                      = Databases.initialize(config)
  val apiSchemaBuilder               = SchemaBuilder()(system, this)
  val projectFetcher: ProjectFetcher = ProjectFetcherImpl(Vector.empty, config)
}

case class ApiDependenciesForTest(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ApiDependencies {
  val databases                      = Databases.initialize(config)
  val apiSchemaBuilder               = SchemaBuilder()(system, this)
  val projectFetcher: ProjectFetcher = ProjectFetcherImpl(Vector.empty, config)
}
