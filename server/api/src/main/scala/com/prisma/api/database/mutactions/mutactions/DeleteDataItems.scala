package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.Types.DataItemFilterCollection
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.shared.models.{Model, Project}

import scala.concurrent.Future

case class DeleteDataItems(
    project: Project,
    model: Model,
    where: DataItemFilterCollection
) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful(
    ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.deleteDataItems(project, model, where))
  )
}
