package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.mutations.CascadingDeletes.{ModelEdge, NodeEdge, Path}
import com.prisma.shared.models.Project
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

case class DeleteDataItemNested(project: Project, path: Path) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DBIO.seq(
          DatabaseMutationBuilder.deleteRelayRow(project.id, path),
          DatabaseMutationBuilder.deleteDataItem(project.id, path)
        )))
  }
}
