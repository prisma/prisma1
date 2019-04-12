package com.prisma.deploy.connector.mongo

import com.prisma.config.DatabaseConfig
import com.prisma.deploy.connector._
import com.prisma.deploy.connector.mongo.impl._
import com.prisma.deploy.connector.persistence.{CloudSecretPersistence, MigrationPersistence, ProjectPersistence, TelemetryPersistence}
import com.prisma.shared.models.{ConnectorCapabilities, Project, ProjectIdEncoder}
import org.joda.time.DateTime
import org.mongodb.scala.MongoClient

import scala.concurrent.{ExecutionContext, Future}

case class MongoDeployConnector(config: DatabaseConfig, isTest: Boolean)(implicit ec: ExecutionContext) extends DeployConnector {
  override def fieldRequirements: FieldRequirementsInterface = FieldRequirementsInterface.empty

  lazy val internalDatabaseDefs     = MongoInternalDatabaseDefs(config)
  lazy val mongoClient: MongoClient = internalDatabaseDefs.client
  lazy val internalDatabase         = mongoClient.getDatabase("prisma")

  override val migrationPersistence: MigrationPersistence       = MongoMigrationPersistence(internalDatabase)
  override val projectPersistence: ProjectPersistence           = MongoProjectPersistence(internalDatabase, migrationPersistence, config)
  override val telemetryPersistence: TelemetryPersistence       = MongoTelemetryPersistence()
  override val cloudSecretPersistence: CloudSecretPersistence   = MongoCloudSecretPersistence(internalDatabase)
  override val deployMutactionExecutor: DeployMutactionExecutor = MongoDeployMutactionExecutor(mongoClient)
  override val projectIdEncoder: ProjectIdEncoder               = ProjectIdEncoder('_')
  override val databaseInspector: DatabaseInspector             = DatabaseInspector.empty
  override def capabilities: ConnectorCapabilities              = ConnectorCapabilities.mongo(isTest = isTest)

  override def clientDBQueries(project: Project): ClientDbQueries = MongoClientDbQueries(project, mongoClient)

  override def initialize(): Future[Unit] = Future.unit

  override def reset(): Future[Unit] = {
    for {
      collectionNames <- internalDatabase.listCollectionNames().toFuture()
      collections     = collectionNames.map(internalDatabase.getCollection)
      _               <- Future.sequence(collections.map(_.drop.toFuture))
    } yield ()
  }

  override def shutdown(): Future[Unit] = {
    mongoClient.close()
    Future.unit
  }

  override def createProjectDatabase(id: String): Future[Unit] = { // This is a hack
    mongoClient.getDatabase(config.database.getOrElse(id)).listCollectionNames().toFuture().map(_ => ())
  }

  override def deleteProjectDatabase(id: String): Future[Unit] = {
    mongoClient.getDatabase(config.database.getOrElse(id)).drop().toFuture().map(_ => ())
  }

  override def getAllDatabaseSizes(): Future[Vector[DatabaseSize]] = Future.successful(Vector.empty)

  override def getOrCreateTelemetryInfo(): Future[TelemetryInfo]       = telemetryPersistence.getOrCreateInfo()
  override def updateTelemetryInfo(lastPinged: DateTime): Future[Unit] = telemetryPersistence.updateTelemetryInfo(lastPinged)

  override def managementLock(): Future[Unit] = Future.successful(())
}
