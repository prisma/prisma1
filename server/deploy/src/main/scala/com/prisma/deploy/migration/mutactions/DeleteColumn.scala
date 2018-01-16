package com.prisma.deploy.migration.mutactions

import com.prisma.deploy.database.DatabaseMutationBuilder
import com.prisma.shared.models.{Field, Model}

import scala.concurrent.Future

case class DeleteColumn(projectId: String, model: Model, field: Field) extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(
      ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.deleteColumn(projectId = projectId, tableName = model.name, columnName = field.name)))
  }

  override def rollback = Some(CreateColumn(projectId, model, field).execute)
}
