package cool.graph.singleserver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.ApiDependencies
import cool.graph.api.database.DatabaseConnectionManager
import cool.graph.api.project.{ProjectFetcher, ProjectFetcherImpl}
import cool.graph.api.schema.SchemaBuilder
import cool.graph.deploy.DeployDependencies

trait SingleServerApiDependencies extends DeployDependencies with ApiDependencies {}

case class SingleServerDependencies(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SingleServerApiDependencies {
  implicit val self = this

  val databaseManager                = DatabaseConnectionManager.initializeForSingleRegion(config)
  val apiSchemaBuilder               = SchemaBuilder()
  val projectFetcher: ProjectFetcher = ProjectFetcherImpl(Vector.empty, config)
}
