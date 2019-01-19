package com.prisma.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.connector.DatabaseMutactionExecutor
import com.prisma.api.mutactions.{DatabaseMutactionVerifierImpl, SideEffectMutactionExecutorImpl}
import com.prisma.api.project.ProjectFetcher
import com.prisma.api.schema.SchemaBuilder
import com.prisma.config.ConfigLoader
import com.prisma.connectors.utils.ConnectorLoader
import com.prisma.deploy.connector.DeployConnector
import com.prisma.messagebus.PubSubSubscriber
import com.prisma.messagebus.testkits.{InMemoryPubSubTestKit, InMemoryQueueTestKit}
import com.prisma.shared.messages.{SchemaInvalidated, SchemaInvalidatedMessage}
import com.prisma.shared.models.ProjectIdEncoder
import com.prisma.subscriptions.Webhook

trait TestApiDependencies extends ApiDependencies {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  def deployConnector: DeployConnector
  def databaseMutactionExecutor: DatabaseMutactionExecutor
  lazy val invalidationTestKit = InMemoryPubSubTestKit[String]()
}

case class TestApiDependenciesImpl()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends TestApiDependencies {
  override implicit def self: ApiDependencies = this

  val config = ConfigLoader.load()

  lazy val apiSchemaBuilder                  = SchemaBuilder()(system, this)
  lazy val projectFetcher: ProjectFetcher    = ???
  override lazy val maxImportExportSize: Int = 1000
  override val invalidationSubscriber: PubSubSubscriber[SchemaInvalidatedMessage] = {
    invalidationTestKit.map[SchemaInvalidatedMessage]((_: String) => SchemaInvalidated)
  }

  override val sssEventsPubSub                  = InMemoryPubSubTestKit[String]()
  override lazy val webhookPublisher            = InMemoryQueueTestKit[Webhook]()
  override lazy val apiConnector                = ConnectorLoader.loadApiConnector(config = config.copy(databases = config.databases.map(_.copy(pooled = false))))
  override lazy val sideEffectMutactionExecutor = SideEffectMutactionExecutorImpl()
  override lazy val mutactionVerifier           = DatabaseMutactionVerifierImpl

  lazy val deployConnector = ConnectorLoader.loadDeployConnector(
    config = config.copy(databases = config.databases.map(_.copy(pooled = false))),
    isTest = true
  )

  override def projectIdEncoder: ProjectIdEncoder = deployConnector.projectIdEncoder
}
