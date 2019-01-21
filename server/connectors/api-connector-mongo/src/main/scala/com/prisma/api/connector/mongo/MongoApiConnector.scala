package com.prisma.api.connector.mongo

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.impl.{MongoDataResolver, MongoDatabaseMutactionExecutor}
import com.prisma.config.DatabaseConfig
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability, Project, ProjectIdEncoder}

import scala.concurrent.{ExecutionContext, Future}

case class MongoApiConnector(config: DatabaseConfig)(implicit ec: ExecutionContext) extends ApiConnector {
  lazy val client = MongoDatabasesFactory.initialize(config)

  override def databaseMutactionExecutor: DatabaseMutactionExecutor = new MongoDatabaseMutactionExecutor(client)

  override def dataResolver(project: Project): DataResolver = MongoDataResolver(project, client)

  override def masterDataResolver(project: Project): DataResolver = MongoDataResolver(project, client)

  override def projectIdEncoder: ProjectIdEncoder = ProjectIdEncoder('_')

  override def capabilities = ConnectorCapabilities.mongo(isTest = false)

  override def initialize(): Future[Unit] = {
    client
    Future.unit
  }

  override def shutdown(): Future[Unit] = {
    client.close()
    Future.unit
  }
}
