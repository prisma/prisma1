package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.{ClientMutactionNoop, ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.shared.models.Field
import com.prisma.shared.models.IdType.Id

import scala.concurrent.Future

case class RemoveDataItemFromManyRelationByFromId(projectId: String, fromField: Field, fromId: Id) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val fromRelationSide = fromField.relationSide.get
    val relation         = fromField.relation.get

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder
          .deleteDataItemByValues(projectId, relation.id, Map(fromRelationSide.toString -> fromId))))
  }

  override def rollback: Some[Future[ClientSqlStatementResult[Any]]] = Some(ClientMutactionNoop().execute)
}
