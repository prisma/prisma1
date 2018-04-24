package com.prisma.deploy.connector

import org.joda.time.DateTime
import com.prisma.shared.models.{Field, Model, Project, ProjectIdEncoder}

import scala.concurrent.Future

trait DeployConnector {
  def projectPersistence: ProjectPersistence
  def migrationPersistence: MigrationPersistence
  def deployMutactionExecutor: DeployMutactionExecutor
  def clientDBQueries(project: Project): ClientDbQueries
  def projectIdEncoder: ProjectIdEncoder

  def initialize(): Future[Unit]
  def reset(): Future[Unit]
  def shutdown(): Future[Unit]

  // other methods
  def createProjectDatabase(id: String): Future[Unit]
  def deleteProjectDatabase(id: String): Future[Unit]
  def getAllDatabaseSizes(): Future[Vector[DatabaseSize]]

  def getOrCreateTelemetryInfo(): Future[TelemetryInfo]
  def updateTelemetryInfo(lastPinged: DateTime): Future[Unit]
}

case class DatabaseSize(name: String, total: Double)
case class TelemetryInfo(id: String, lastPing: Option[DateTime])

trait ClientDbQueries {
  def existsByModel(modelName: String): Future[Boolean]
  def existsByRelation(relationId: String): Future[Boolean]
  def existsNullByModelAndField(model: Model, field: Field): Future[Boolean]
  def enumValueIsInUse(models: Vector[Model], enumName: String, value: String): Future[Boolean]
}
