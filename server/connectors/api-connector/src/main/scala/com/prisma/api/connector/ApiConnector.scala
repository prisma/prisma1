package com.prisma.api.connector

import com.prisma.api.connector.ApiConnectorCapability.ScalarListsCapability
import com.prisma.shared.models.{Project, ProjectIdEncoder}
import play.api.libs.json.JsValue

import scala.concurrent.Future
import enumeratum._

trait ApiConnector {
  def databaseMutactionExecutor: DatabaseMutactionExecutor
  def dataResolver(project: Project): DataResolver
  def masterDataResolver(project: Project): DataResolver
  def projectIdEncoder: ProjectIdEncoder
  def capabilities: Set[ApiConnectorCapability]

  def hasCapability(capability: ApiConnectorCapability): Boolean = {
    capability match {
      case ScalarListsCapability => capabilities.exists(_.isInstanceOf[ScalarListsCapability])
      case c                     => capabilities.contains(c)
    }
  }

  def initialize(): Future[Unit]
  def shutdown(): Future[Unit]
}

case class MutactionResults2(
    databaseResult: DatabaseMutactionResult,
    nestedResults: Vector[MutactionResults2]
) {
  def allResults: Vector[DatabaseMutactionResult] = databaseResult +: nestedResults.flatMap(_.allResults)

  def getExecuted: Vector[DatabaseMutaction] = {
    nestedResults.map(_.databaseResult.mutaction)
  }
}

case class MutactionResults(results: Vector[DatabaseMutactionResult])

trait DatabaseMutactionExecutor {
  def executeTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults]
  def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults]

  def executeRaw(query: String): Future[JsValue]
}

sealed trait ApiConnectorCapability extends EnumEntry

object ApiConnectorCapability extends Enum[ApiConnectorCapability] {
  val values = findValues

  sealed trait ScalarListsCapability         extends ApiConnectorCapability
  object ScalarListsCapability               extends ScalarListsCapability
  object EmbeddedScalarListsCapability       extends ScalarListsCapability
  object NonEmbeddedScalarListCapability     extends ScalarListsCapability
  object NodeQueryCapability                 extends ApiConnectorCapability
  object EmbeddedTypesCapability             extends ApiConnectorCapability
  object JoinRelationsCapability             extends ApiConnectorCapability // this means normal relations, e.g. relations between documents in MongoDB
  object ImportExportCapability              extends ApiConnectorCapability
  object TransactionalExecutionCapability    extends ApiConnectorCapability
  object SupportsExistingDatabasesCapability extends ApiConnectorCapability
  // HandlesInvalidDataCapability  -> for Mongo
  // RawAccessCapability

}
