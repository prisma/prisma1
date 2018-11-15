package com.prisma.subscriptions
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.mutactions.{DatabaseMutactionVerifierImpl, SideEffectMutactionExecutorImpl}
import com.prisma.api.project.ProjectFetcher
import com.prisma.api.schema.SchemaBuilder
import com.prisma.api.{ApiDependencies, TestApiDependencies}
import com.prisma.cache.Cache
import com.prisma.config.ConfigLoader
import com.prisma.connectors.utils.ConnectorLoader
import com.prisma.messagebus.testkits.InMemoryPubSubTestKit
import com.prisma.messagebus.{PubSubPublisher, PubSubSubscriber}
import com.prisma.shared.messages.{SchemaInvalidated, SchemaInvalidatedMessage}
import com.prisma.shared.models.{Project, ProjectIdEncoder}

import scala.concurrent.Future

class TestSubscriptionDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer)
    extends SubscriptionDependencies
    with TestApiDependencies {
  override implicit def self: ApiDependencies = this

  val config = ConfigLoader.load()

  lazy val deployConnector = ConnectorLoader.loadDeployConnector(config.copy(databases = config.databases.map(_.copy(pooled = false))))

  override lazy val invalidationTestKit = InMemoryPubSubTestKit[String]()
  lazy val sssEventsTestKit             = InMemoryPubSubTestKit[String]()

  override val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] = {
    invalidationTestKit.map[SchemaInvalidatedMessage]((_: String) => SchemaInvalidated)
  }

  override lazy val sssEventsPublisher: PubSubPublisher[String] = sssEventsTestKit
  override val sssEventsSubscriber: PubSubSubscriber[String]    = sssEventsTestKit

  override val keepAliveIntervalSeconds = 1000

  override val projectFetcher: TestProjectFetcher = TestProjectFetcher()

  override lazy val apiSchemaBuilder: SchemaBuilder = ???
  override lazy val sssEventsPubSub                 = ???
  override lazy val webhookPublisher                = ???

  override lazy val apiConnector = ConnectorLoader.loadApiConnector(config)

  override def projectIdEncoder: ProjectIdEncoder = apiConnector.projectIdEncoder

  override lazy val sideEffectMutactionExecutor = SideEffectMutactionExecutorImpl()
  override lazy val mutactionVerifier           = DatabaseMutactionVerifierImpl
}

case class TestProjectFetcher() extends ProjectFetcher {
  val cache = Cache.unbounded[String, Project]()

  override def fetch(projectIdOrAlias: String) = Future.successful(cache.get(projectIdOrAlias))

  def put(projectIdOrAlias: String, project: Project) = cache.put(projectIdOrAlias, project)
}
