package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, MutactionVerificationSuccess}
import com.prisma.api.mutations.mutations.CascadingDeletes.Path
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.Project
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class DeleteDataItem(project: Project, path: Path, previousValues: DataItem, id: String) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DBIO.seq(
          DatabaseMutationBuilder.deleteRelayRowByPath(project.id, path),
          DatabaseMutationBuilder.deleteDataItemByPath(project.id, path)
        )
      )
    )
  }

  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {
    resolver.existsByWhere(path.root) map {
      case false => Failure(APIErrors.NodeNotFoundForWhereError(path.root))
      case true  => Success(MutactionVerificationSuccess())
    }
  }
}
