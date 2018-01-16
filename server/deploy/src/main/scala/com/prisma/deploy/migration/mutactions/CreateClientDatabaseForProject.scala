package com.prisma.deploy.migration.mutactions

import com.prisma.deploy.database.DatabaseMutationBuilder

import scala.concurrent.Future

case class CreateClientDatabaseForProject(projectId: String) extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] =
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.createClientDatabaseForProject(projectId = projectId)))

  override def rollback = Some(DeleteClientDatabaseForProject(projectId).execute)
}
