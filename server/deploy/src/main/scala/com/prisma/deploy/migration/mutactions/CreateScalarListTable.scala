package com.prisma.deploy.migration.mutactions

import com.prisma.deploy.database.DatabaseMutationBuilder
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier

import scala.concurrent.Future

case class CreateScalarListTable(projectId: String, model: String, field: String, typeIdentifier: TypeIdentifier) extends ClientSqlMutaction {
  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(
      sqlAction = DatabaseMutationBuilder.createScalarListTable(projectId = projectId, modelName = model, fieldName = field, typeIdentifier = typeIdentifier)))
  }

  override def rollback = Some(DeleteScalarListTable(projectId, model, field, typeIdentifier).execute)
}
