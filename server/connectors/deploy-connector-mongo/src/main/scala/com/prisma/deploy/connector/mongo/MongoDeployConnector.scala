package com.prisma.deploy.connector.mongo

import com.prisma.config.DatabaseConfig
import com.prisma.deploy.connector._
import com.prisma.deploy.connector.mongo.impl._
import com.prisma.deploy.connector.persistence.{CloudSecretPersistence, MigrationPersistence, ProjectPersistence, TelemetryPersistence}
import com.prisma.shared.models.ApiConnectorCapability.{EmbeddedScalarListsCapability, JoinRelationsCapability, MigrationsCapability, MongoRelationsCapability}
import com.prisma.shared.models.{ConnectorCapability, Project, ProjectIdEncoder}
import org.joda.time.DateTime
import org.mongodb.scala.MongoClient

import scala.concurrent.{ExecutionContext, Future}

case class MongoDeployConnector(config: DatabaseConfig, isActive: Boolean)(implicit ec: ExecutionContext) extends DeployConnector {
  override def fieldRequirements: FieldRequirementsInterface = FieldRequirementImpl(isActive)

  lazy val internalDatabaseDefs     = MongoInternalDatabaseDefs(config)
  lazy val mongoClient: MongoClient = internalDatabaseDefs.client
  lazy val internalDatabase         = mongoClient.getDatabase("prisma")

  override val migrationPersistence: MigrationPersistence     = MigrationPersistenceImpl(internalDatabase)
  override val projectPersistence: ProjectPersistence         = ProjectPersistenceImpl(internalDatabase, migrationPersistence)
  override val telemetryPersistence: TelemetryPersistence     = MongoTelemetryPersistence()
  override val cloudSecretPersistence: CloudSecretPersistence = CloudSecretPersistenceImpl(internalDatabase)

  override val deployMutactionExecutor: DeployMutactionExecutor = MongoDeployMutactionExecutor(mongoClient)
  override val projectIdEncoder: ProjectIdEncoder               = ProjectIdEncoder('_')
  override def capabilities: Set[ConnectorCapability] =
    if (isActive) Set(MigrationsCapability, EmbeddedScalarListsCapability, JoinRelationsCapability, MongoRelationsCapability)
    else Set(EmbeddedScalarListsCapability, JoinRelationsCapability, MongoRelationsCapability)

  override def clientDBQueries(project: Project): ClientDbQueries                              = MongoClientDbQueries(project, mongoClient)
  override def databaseIntrospectionInferrer(projectId: String): DatabaseIntrospectionInferrer = EmptyDatabaseIntrospectionInferrer

  override def initialize(): Future[Unit] = Future.unit

  override def reset(): Future[Unit] = {
    for {
      collectionNames <- internalDatabase.listCollectionNames().toFuture()
      collections     = collectionNames.map(internalDatabase.getCollection)
      _               <- Future.sequence(collections.map(_.drop.toFuture))
    } yield ()
  }

  override def shutdown(): Future[Unit] = Future.unit

  override def createProjectDatabase(id: String): Future[Unit] = { // This is a hack
    mongoClient.getDatabase(id).listCollectionNames().toFuture().map(_ -> Unit)
  }

  override def deleteProjectDatabase(id: String): Future[Unit] = {
    mongoClient.getDatabase(id).drop().toFuture().map(_ -> Unit)
  }

  override def getAllDatabaseSizes(): Future[Vector[DatabaseSize]] = Future.successful(Vector.empty)

  override def getOrCreateTelemetryInfo(): Future[TelemetryInfo]       = telemetryPersistence.getOrCreateInfo()
  override def updateTelemetryInfo(lastPinged: DateTime): Future[Unit] = telemetryPersistence.updateTelemetryInfo(lastPinged)
}
