package com.prisma.deploy.migration.mutactions

import com.prisma.deploy.database.DatabaseMutationBuilder
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

case class DeleteModelTable(projectId: String, model: String, scalarListFields: Vector[String]) extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {

    val dropTable            = DatabaseMutationBuilder.dropTable(projectId = projectId, tableName = model)
    val dropScalarListFields = scalarListFields.map(field => DatabaseMutationBuilder.dropScalarListTable(projectId, model, field))

    Future.successful(ClientSqlStatementResult(sqlAction = DBIO.seq(dropScalarListFields :+ dropTable: _*)))
  }

  override def rollback = Some(CreateModelTable(projectId, model).execute)
}
