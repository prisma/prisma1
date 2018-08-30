package com.prisma.api.connector.mongo

import com.prisma.api.connector.ApiConnectorCapability.{EmbeddedScalarListsCapability, EmbeddedTypesCapability, NodeQueryCapability}
import com.prisma.api.connector._
import com.prisma.api.connector.mongo.impl.{MongoDataResolver, MongoDatabaseMutactionExecutor}
import com.prisma.config.DatabaseConfig
import com.prisma.shared.models.{Project, ProjectIdEncoder}

import scala.concurrent.{ExecutionContext, Future}

case class MongoApiConnector(config: DatabaseConfig)(implicit ec: ExecutionContext) extends ApiConnector {
  lazy val client = MongoDatabasesFactory.initialize(config)

  override def databaseMutactionExecutor: DatabaseMutactionExecutor = new MongoDatabaseMutactionExecutor(client)

  override def dataResolver(project: Project): DataResolver = new MongoDataResolver(project, client)

  override def masterDataResolver(project: Project): DataResolver = new MongoDataResolver(project, client)

  override def projectIdEncoder: ProjectIdEncoder = ???

  override def capabilities: Set[ApiConnectorCapability] = Set(NodeQueryCapability, EmbeddedScalarListsCapability, EmbeddedTypesCapability)

  override def initialize(): Future[Unit] = {
    client
    Future.unit
  }

  override def shutdown(): Future[Unit] = {
    Future.unit
  }
}
