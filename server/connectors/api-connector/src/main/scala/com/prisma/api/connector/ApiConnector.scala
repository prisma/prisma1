package com.prisma.api.connector

import com.prisma.shared.models.{Project, ProjectIdEncoder}

import scala.concurrent.Future

trait ApiConnector {
  def databaseMutactionExecutor: DatabaseMutactionExecutor
  def dataResolver(project: Project): DataResolver
  def masterDataResolver(project: Project): DataResolver
  def projectIdEncoder: ProjectIdEncoder
  def capabilities: Vector[ApiConnectorCapability]

  def initialize(): Future[Unit]
  def shutdown(): Future[Unit]
}

case class MutactionResults(
    databaseResult: DatabaseMutactionResult,
    nestedResults: Vector[MutactionResults]
) {
  def allResults: Vector[DatabaseMutactionResult] = databaseResult +: nestedResults.flatMap(_.allResults)

  def getExecuted: Vector[DatabaseMutaction] = {
    nestedResults.map(_.databaseResult.mutaction)
  }
}

case class MutactionResults2(
    map: Map[DatabaseMutaction, DatabaseMutactionResult]
) {
  def allResults: Vector[DatabaseMutactionResult] = map.values.toVector
}

trait DatabaseMutactionExecutor {
  def executeTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults]
  def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults]
}

sealed trait ApiConnectorCapability
object NodeQueryCapability extends ApiConnectorCapability
