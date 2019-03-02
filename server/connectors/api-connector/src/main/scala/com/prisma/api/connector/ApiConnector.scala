package com.prisma.api.connector

import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.ConnectorCapability.ScalarListsCapability
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability, Project, ProjectIdEncoder}
import play.api.libs.json.JsValue

import scala.concurrent.Future

trait ApiConnector {
  def databaseMutactionExecutor: DatabaseMutactionExecutor
  def dataResolver(project: Project): DataResolver
  def masterDataResolver(project: Project): DataResolver
  def projectIdEncoder: ProjectIdEncoder
  def capabilities: ConnectorCapabilities

  def initialize(): Future[Unit]
  def shutdown(): Future[Unit]
}

case class MutactionResults(results: Vector[DatabaseMutactionResult]) {
  def merge(otherResult: MutactionResults): MutactionResults          = MutactionResults(results ++ otherResult.results)
  def merge(otherResults: Vector[MutactionResults]): MutactionResults = MutactionResults(results ++ otherResults.flatMap(_.results))
  def id(m: FurtherNestedMutaction): IdGCValue                        = results.find(_.mutaction.id == m.id).get.asInstanceOf[FurtherNestedMutactionResult].id
  def nodeAddress(m: FurtherNestedMutaction): NodeAddress             = results.find(_.mutaction.id == m.id).get.asInstanceOf[FurtherNestedMutactionResult].nodeAddress
  def contains(m: DatabaseMutaction): Boolean                         = results.map(_.mutaction.id).contains(m.id)
}

trait DatabaseMutactionExecutor {
  def executeTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults]
  def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults]
  def executeRaw(project: Project, query: String): Future[JsValue]
}
