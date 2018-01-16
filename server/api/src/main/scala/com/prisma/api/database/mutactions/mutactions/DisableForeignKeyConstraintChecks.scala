package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.{ClientSqlMutaction, ClientSqlStatementResult}

import scala.concurrent.Future

case class DisableForeignKeyConstraintChecks() extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] =
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.disableForeignKeyConstraintChecks))

}
