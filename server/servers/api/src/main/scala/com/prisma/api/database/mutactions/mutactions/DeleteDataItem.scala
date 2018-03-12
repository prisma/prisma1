package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, MutactionVerificationSuccess}
import com.prisma.api.mutations.mutations.CascadingDeletes.Path
import com.prisma.shared.models.Project
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future
import scala.util.{Success, Try}

case class DeleteDataItem(project: Project, path: Path, previousValues: DataItem, id: String) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DBIO.seq(
          DatabaseMutationBuilder.deleteRelayRow(project.id, path),
          DatabaseMutationBuilder.deleteDataItem(project.id, path)
        )
      )
    )
  }
}
