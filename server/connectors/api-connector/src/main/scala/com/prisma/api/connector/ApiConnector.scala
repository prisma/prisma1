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

trait DatabaseMutactionExecutor {
  def execute(mutactions: Vector[DatabaseMutaction], runTransactionally: Boolean = true): Future[Vector[DatabaseMutactionResult]]
}

sealed trait ApiConnectorCapability
object NodeQueryCapability extends ApiConnectorCapability
