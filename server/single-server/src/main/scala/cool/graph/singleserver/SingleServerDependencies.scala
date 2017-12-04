package cool.graph.singleserver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.ApiDependencies
import cool.graph.api.database.Databases
import cool.graph.api.project.{ProjectFetcher, ProjectFetcherImpl}
import cool.graph.api.schema.SchemaBuilder
import cool.graph.deploy.DeployDependencies

trait SingleServerApiDependencies extends DeployDependencies with ApiDependencies {}

case class SingleServerDependencies(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SingleServerApiDependencies {
  override implicit def self = this

  def init: Unit = {
    migrationApplierJob
  }

  val databases                      = Databases.initialize(config)
  val apiSchemaBuilder               = SchemaBuilder()
  val projectFetcher: ProjectFetcher = ProjectFetcherImpl(Vector.empty, config)
}
