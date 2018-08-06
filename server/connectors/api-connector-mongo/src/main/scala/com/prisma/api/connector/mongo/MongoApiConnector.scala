package com.prisma.api.connector.mongo

import com.prisma.api.connector._
import com.prisma.config.DatabaseConfig
import com.prisma.shared.models.{Project, ProjectIdEncoder}

import scala.concurrent.{ExecutionContext, Future}

case class MongoApiConnector(config: DatabaseConfig)(implicit ec: ExecutionContext) extends ApiConnector {
  lazy val database = MongoDatabasesFactory.database

  override def databaseMutactionExecutor: DatabaseMutactionExecutor = new MongoDatabaseMutactionExecutor(database)

  override def dataResolver(project: Project): DataResolver = new MongoDataResolver(project, database)

  override def masterDataResolver(project: Project): DataResolver = new MongoDataResolver(project, database)

  override def projectIdEncoder: ProjectIdEncoder = ???

  override def capabilities: Vector[ApiConnectorCapability] = Vector.empty

  override def initialize(): Future[Unit] = ???

  override def shutdown(): Future[Unit] = ???
}
