package com.prisma.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.connector.postgresql.PostgresApiConnector
import com.prisma.api.mutactions.{DatabaseMutactionVerifierImpl, SideEffectMutactionExecutorImpl}
import com.prisma.api.project.ProjectFetcher
import com.prisma.api.schema.SchemaBuilder
import com.prisma.config.ConfigLoader
import com.prisma.deploy.connector.postgresql.PostgresDeployConnector
import com.prisma.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import com.prisma.messagebus.testkits.InMemoryQueueTestKit
import com.prisma.subscriptions.Webhook

import scala.util.{Failure, Success}

case class TestApiDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ApiDependencies {
  override implicit def self: ApiDependencies = this

  val config = ConfigLoader.load() match {
    case Success(c)   => c
    case Failure(err) => sys.error(s"Unable to load Prisma config: $err")
  }

  lazy val apiSchemaBuilder                     = SchemaBuilder()(system, this)
  lazy val projectFetcher: ProjectFetcher       = ???
  override lazy val maxImportExportSize: Int    = 1000
  override lazy val sssEventsPubSub             = InMemoryAkkaPubSub[String]()
  override lazy val webhookPublisher            = InMemoryQueueTestKit[Webhook]()
  override lazy val apiConnector                = PostgresApiConnector(config.databases.head)
  override lazy val sideEffectMutactionExecutor = SideEffectMutactionExecutorImpl()
  override lazy val mutactionVerifier           = DatabaseMutactionVerifierImpl

  lazy val deployConnector = PostgresDeployConnector(config.databases.head)
}
