package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.NodeSelector
import com.prisma.shared.models._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

case class SetScalarListToEmpty(
    project: Project,
    where: NodeSelector,
    field: Field
) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DBIO.seq(
          DatabaseMutationBuilder.setScalarListToEmpty(project.id, where, field.name)
        )))
  }
}
