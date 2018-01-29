package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, MutactionVerificationSuccess}
import com.prisma.api.mutations.NodeSelector
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.Project
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class DeleteDataItem(project: Project, where: NodeSelector, previousValues: DataItem, id: String, requestId: Option[String] = None)
    extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DBIO.seq(
          DatabaseMutationBuilder.deleteRelayRowByUnique(project.id, where),
          DatabaseMutationBuilder.deleteDataItemByUnique(project.id, where)
        )
      )
    )
  }

  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {
    resolver.existsByWhere(where) map {
      case false => Failure(APIErrors.NodeNotFoundForWhereError(where))
      case true  => Success(MutactionVerificationSuccess())
    }
  }
}
