package com.prisma.deploy.connector

import com.prisma.deploy.connector.persistence.{CloudSecretPersistence, MigrationPersistence, ProjectPersistence, TelemetryPersistence}
import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models._
import org.joda.time.DateTime

import scala.concurrent.Future

trait DeployConnector {
  def projectPersistence: ProjectPersistence
  def migrationPersistence: MigrationPersistence
  def cloudSecretPersistence: CloudSecretPersistence
  def telemetryPersistence: TelemetryPersistence

  def deployMutactionExecutor: DeployMutactionExecutor
  def clientDBQueries(project: Project): ClientDbQueries
  def projectIdEncoder: ProjectIdEncoder
  def databaseInspector: DatabaseInspector
  def capabilities: ConnectorCapabilities

  def initialize(): Future[Unit]
  def reset(): Future[Unit]
  def shutdown(): Future[Unit]

  // other methods
  def createProjectDatabase(id: String): Future[Unit]
  def deleteProjectDatabase(id: String): Future[Unit]
  def getAllDatabaseSizes(): Future[Vector[DatabaseSize]]

  def getOrCreateTelemetryInfo(): Future[TelemetryInfo]
  def updateTelemetryInfo(lastPinged: DateTime): Future[Unit]

  // Database level lock granting exclusive deploy privileges to this instance.
  def managementLock(): Future[Unit]
}

case class DatabaseSize(name: String, total: Double)
case class TelemetryInfo(id: String, lastPing: Option[DateTime])

trait ClientDbQueries {
  def existsByModel(model: Model): Future[Boolean]
  def existsByRelation(relation: Relation): Future[Boolean]
  def existsDuplicateByRelationAndSide(relation: Relation, side: RelationSide): Future[Boolean]
  def existsNullByModelAndField(model: Model, field: Field): Future[Boolean]
  def existsDuplicateValueByModelAndField(model: Model, field: ScalarField): Future[Boolean]
  def enumValueIsInUse(models: Vector[Model], enumName: String, value: String): Future[Boolean]
}

object EmptyClientDbQueries extends ClientDbQueries {
  private val falseFuture = Future.successful(false)

  override def existsByModel(model: Model)                                              = falseFuture
  override def existsByRelation(relation: Relation)                                     = falseFuture
  override def existsDuplicateByRelationAndSide(relation: Relation, side: RelationSide) = falseFuture
  override def existsNullByModelAndField(model: Model, field: Field)                    = falseFuture
  override def existsDuplicateValueByModelAndField(model: Model, field: ScalarField)    = falseFuture
  override def enumValueIsInUse(models: Vector[Model], enumName: String, value: String) = falseFuture
}
