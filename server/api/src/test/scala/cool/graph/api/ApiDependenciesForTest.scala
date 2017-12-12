package cool.graph.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.database.Databases
import cool.graph.api.project.{ProjectFetcher, ProjectFetcherImpl}
import cool.graph.api.schema.SchemaBuilder

case class ApiDependenciesForTest(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ApiDependencies {
  override implicit def self: ApiDependencies = this

  val databases                      = Databases.initialize(config)
  val apiSchemaBuilder               = SchemaBuilder()(system, this)
  val projectFetcher: ProjectFetcher = ProjectFetcherImpl(Vector.empty, config)
  override lazy val maxImportExportSize: Int = 1000

}
