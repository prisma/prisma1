package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.{ClientMutactionNoop, ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.Project

import scala.concurrent.Future

case class RemoveDataItemFromRelationById(project: Project, relationId: String, id: Id) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.deleteRelationRowById(project.id, relationId, id)))
  }

  override def rollback: Some[Future[ClientSqlStatementResult[Any]]] = Some(ClientMutactionNoop().execute)
}
