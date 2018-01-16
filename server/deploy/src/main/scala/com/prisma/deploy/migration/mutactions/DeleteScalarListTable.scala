package com.prisma.deploy.migration.mutactions

import com.prisma.deploy.database.DatabaseMutationBuilder
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

case class DeleteScalarListTable(projectId: String, model: String, field: String, typeIdentifier: TypeIdentifier) extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(
      ClientSqlStatementResult(sqlAction = DBIO.seq(DatabaseMutationBuilder.dropScalarListTable(projectId = projectId, modelName = model, fieldName = field))))
  }

  override def rollback = Some(CreateScalarListTable(projectId, model, field, typeIdentifier).execute)
}
