package com.prisma.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.connector.mysql.ApiConnectorImpl
import com.prisma.api.connector.mysql.database.Databases
import com.prisma.api.mutactions.{DatabaseMutactionVerifierImpl, SideEffectMutactionExecutorImpl}
import com.prisma.api.project.ProjectFetcher
import com.prisma.api.schema.SchemaBuilder
import com.prisma.subscriptions.Webhook
import com.prisma.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import com.prisma.messagebus.testkits.InMemoryQueueTestKit

case class ApiDependenciesForTest()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ApiDependencies {
  override implicit def self: ApiDependencies = this

  val databases                              = Databases.initialize(config)
  val apiSchemaBuilder                       = SchemaBuilder()(system, this)
  lazy val projectFetcher: ProjectFetcher    = ???
  override lazy val maxImportExportSize: Int = 1000
  override val sssEventsPubSub               = InMemoryAkkaPubSub[String]()
  override val webhookPublisher              = InMemoryQueueTestKit[Webhook]()
  override def apiConnector                  = ApiConnectorImpl(databases.master)
  override def sideEffectMutactionExecutor   = SideEffectMutactionExecutorImpl()
  override def mutactionVerifier             = DatabaseMutactionVerifierImpl
}
