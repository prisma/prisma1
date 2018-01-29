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

case class DeleteDataItemNested(project: Project, where: NodeSelector) extends ClientSqlDataChangeMutaction {

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
}
