package com.prisma.deploy.connector.mongo

import com.prisma.config.DatabaseConfig
import com.prisma.deploy.connector._
import com.prisma.deploy.connector.mongo.impl.{CloudSecretPersistenceImpl, MigrationPersistenceImpl, MongoDeployMutactionExecutor, ProjectPersistenceImpl}
import com.prisma.shared.models.{Project, ProjectIdEncoder}
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

case class MongoDeployConnector(config: DatabaseConfig)(implicit ec: ExecutionContext) extends DeployConnector {
  lazy val internalDatabaseDefs = MongoInternalDatabaseDefs(config)
  lazy val mongoClient          = internalDatabaseDefs.client
  lazy val internalDatabase     = mongoClient.getDatabase("prisma")

  override def isActive: Boolean = true

  override def projectPersistence: ProjectPersistence = ProjectPersistenceImpl(internalDatabase)

  override def migrationPersistence: MigrationPersistence = MigrationPersistenceImpl(internalDatabase)

  override def deployMutactionExecutor: DeployMutactionExecutor = MongoDeployMutactionExecutor(mongoClient)

  override def clientDBQueries(project: Project): ClientDbQueries = EmptyClientDbQueries

  override def projectIdEncoder: ProjectIdEncoder = ProjectIdEncoder('$')

  override def databaseIntrospectionInferrer(projectId: String): DatabaseIntrospectionInferrer = EmptyDatabaseIntrospectionInferrer

  override def cloudSecretPersistence: CloudSecretPersistence = CloudSecretPersistenceImpl(internalDatabase)

  override def initialize(): Future[Unit] = Future.unit

  override def reset(): Future[Unit] = {
    val collections = Vector(internalDatabase.getCollection("Migration"), internalDatabase.getCollection("Project"), internalDatabase.getCollection("Secret"))
    val dropResult  = Future.sequence(collections.map(_.drop.toFuture))
    dropResult.map(_ => ())
  }

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
