package com.prisma.deploy.migration.mutactions

import com.prisma.deploy.database.DatabaseMutationBuilder
import com.prisma.deploy.schema.InvalidName
import com.prisma.deploy.validation.NameConstraints
import com.prisma.shared.models.Model

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class CreateModelTable(projectId: String, model: String) extends ClientSqlMutaction {
  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.createTable(projectId = projectId, name = model)))
  }

  override def rollback = Some(DeleteModelTable(projectId, model, Vector.empty).execute)

  override def verify(): Future[Try[Unit]] = {
    val validationResult = if (NameConstraints.isValidModelName(model)) {
      Success(())
    } else {
      Failure(InvalidName(name = model, entityType = " model"))
    }

    Future.successful(validationResult)
  }
}
