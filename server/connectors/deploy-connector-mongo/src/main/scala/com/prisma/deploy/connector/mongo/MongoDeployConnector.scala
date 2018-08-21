package com.prisma.deploy.connector.mongo

import com.prisma.config.DatabaseConfig
import com.prisma.deploy.connector._
import com.prisma.deploy.connector.mongo.database.{CodecRegistry, MigrationDefinition}
import com.prisma.deploy.connector.mongo.impl.{CloudSecretPersistenceImpl, MigrationPersistenceImpl, MongoDeployMutactionExecutor, ProjectPersistenceImpl}
import com.prisma.shared.models.{Project, ProjectIdEncoder}
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

case class MongoDeployConnector(config: DatabaseConfig)(implicit ec: ExecutionContext) extends DeployConnector {
  lazy val internalDatabaseDefs = MongoInternalDatabaseDefs(config)
  lazy val mongoClient          = internalDatabaseDefs.client
  lazy val internalDatabase     = mongoClient.getDatabase("prisma").withCodecRegistry(CodecRegistry.codecRegistry)

  override def isActive: Boolean = true

  override def projectPersistence: ProjectPersistence = new ProjectPersistenceImpl(internalDatabase)

  override def migrationPersistence: MigrationPersistence = new MigrationPersistenceImpl(internalDatabase)

  override def deployMutactionExecutor: DeployMutactionExecutor = MongoDeployMutactionExecutor(mongoClient)

  override def clientDBQueries(project: Project): ClientDbQueries = ???

  override def projectIdEncoder: ProjectIdEncoder = ProjectIdEncoder('$')

  override def databaseIntrospectionInferrer(projectId: String): DatabaseIntrospectionInferrer = ???

  override def cloudSecretPersistence: CloudSecretPersistence = new CloudSecretPersistenceImpl(internalDatabase)

  override def initialize(): Future[Unit] = Future.unit

  override def reset(): Future[Unit] = Future.unit

  override def shutdown(): Future[Unit] = Future.unit

  override def createProjectDatabase(id: String): Future[Unit] = { // This is a hack
    mongoClient.getDatabase(id).listCollectionNames().toFuture().map(_ -> Unit)
  }

  override def deleteProjectDatabase(id: String): Future[Unit] = {
    mongoClient.getDatabase(id).drop().toFuture().map(_ -> Unit)
  }

  override def getAllDatabaseSizes(): Future[Vector[DatabaseSize]] = Future.successful(Vector.empty)

  override def getOrCreateTelemetryInfo(): Future[TelemetryInfo] = Future.successful(TelemetryInfo("not Implemented", None))

  override def updateTelemetryInfo(lastPinged: DateTime): Future[Unit] = Future.unit
}
