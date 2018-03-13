package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.connector.Path
import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.CoolArgs
import com.prisma.shared.models.Project

import scala.concurrent.Future

case class UpdateDataItemIfInRelationWith(project: Project, path: Path, args: CoolArgs) extends ClientSqlDataChangeMutaction {

  val scalarArgs = args.nonListScalarArguments(path.lastModel)

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
    ClientSqlStatementResult(DatabaseMutationBuilder.updateDataItemByPath(project.id, path, scalarArgs))
  }
}
