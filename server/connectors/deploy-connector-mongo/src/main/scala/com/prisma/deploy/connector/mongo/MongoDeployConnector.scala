package com.prisma.deploy.connector.mongo

import com.prisma.config.DatabaseConfig
import com.prisma.deploy.connector._
import com.prisma.shared.models.{Project, ProjectIdEncoder}
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

case class MongoDeployConnector(config: DatabaseConfig)(implicit ec: ExecutionContext) extends DeployConnector {
  override def isActive: Boolean = false

  override def projectPersistence: ProjectPersistence = ???

  override def migrationPersistence: MigrationPersistence = ???

  override def deployMutactionExecutor: DeployMutactionExecutor = new DeployMutactionExecutor {
    override def execute(mutaction: DeployMutaction): Future[Unit] = Future.unit

    override def rollback(mutaction: DeployMutaction): Future[Unit] = Future.unit
  }

  override def clientDBQueries(project: Project): ClientDbQueries = ???

  override def projectIdEncoder: ProjectIdEncoder = ???

  override def databaseIntrospectionInferrer(projectId: String): DatabaseIntrospectionInferrer = ???

  override def cloudSecretPersistence: CloudSecretPersistence = ???

  override def initialize(): Future[Unit] = Future.unit

  override def reset(): Future[Unit] = Future.unit

  override def shutdown(): Future[Unit] = Future.unit

  override def createProjectDatabase(id: String): Future[Unit] = Future.unit

  override def deleteProjectDatabase(id: String): Future[Unit] = Future.unit

  override def getAllDatabaseSizes(): Future[Vector[DatabaseSize]] = Future.successful(Vector.empty)

  override def getOrCreateTelemetryInfo(): Future[TelemetryInfo] = Future.successful(TelemetryInfo("not Implemented", None))

  override def updateTelemetryInfo(lastPinged: DateTime): Future[Unit] = Future.unit
}
