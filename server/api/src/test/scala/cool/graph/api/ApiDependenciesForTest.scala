package cool.graph.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.database.Databases
import cool.graph.api.project.{ProjectFetcher, ProjectFetcherImpl}
import cool.graph.api.schema.SchemaBuilder
import cool.graph.api.subscriptions.Webhook
import cool.graph.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import cool.graph.messagebus.testkits.InMemoryQueueTestKit

case class ApiDependenciesForTest()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ApiDependencies {
  override implicit def self: ApiDependencies = this

  val databases                              = Databases.initialize(config)
  val apiSchemaBuilder                       = SchemaBuilder()(system, this)
  lazy val projectFetcher: ProjectFetcher    = ???
  override lazy val maxImportExportSize: Int = 1000
  override val sssEventsPubSub               = InMemoryAkkaPubSub[String]()
  override val webhookPublisher              = InMemoryQueueTestKit[Webhook]()
}
