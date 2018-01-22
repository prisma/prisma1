package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.Types.DataItemFilterCollection
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.shared.models.{Model, Project}
import slick.dbio.DBIOAction

import scala.concurrent.Future

case class DeleteDataItems(project: Project, model: Model, whereFilter: DataItemFilterCollection) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful(
    ClientSqlStatementResult(
      sqlAction = DBIOAction.seq(DatabaseMutationBuilder.deleteRelayIds(project, model, whereFilter),
                                 DatabaseMutationBuilder.deleteDataItems(project, model, whereFilter)))
  )
}
