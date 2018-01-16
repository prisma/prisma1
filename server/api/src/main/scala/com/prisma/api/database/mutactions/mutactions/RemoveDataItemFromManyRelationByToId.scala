package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.{DataResolver, DatabaseMutationBuilder}
import com.prisma.api.database.mutactions.{ClientMutactionNoop, ClientSqlDataChangeMutaction, ClientSqlStatementResult, MutactionVerificationSuccess}
import com.prisma.shared.models.Field
import com.prisma.shared.models.IdType.Id

import scala.concurrent.Future
import scala.util.{Success, Try}

case class RemoveDataItemFromManyRelationByToId(projectId: String, fromField: Field, toId: Id) extends ClientSqlDataChangeMutaction {

  override def execute = {
    val toRelationSide = fromField.oppositeRelationSide.get
    val relation       = fromField.relation.get

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder
          .deleteDataItemByValues(projectId, relation.id, Map(toRelationSide.toString -> toId))))
  }

  override def rollback = {
    Some(ClientMutactionNoop().execute)
  }

  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {

    // note: we intentionally don't require that a relation actually exists

    Future.successful(Success(MutactionVerificationSuccess()))
  }
}
