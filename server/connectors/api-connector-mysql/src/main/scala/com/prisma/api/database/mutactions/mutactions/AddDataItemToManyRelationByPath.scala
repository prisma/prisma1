package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.connector.Path
import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.shared.models._

import scala.concurrent.Future

case class AddDataItemToManyRelationByPath(project: Project, path: Path) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {

    ClientSqlStatementResult(DatabaseMutationBuilder.createRelationRowByPath(project.id, path))
  }
}
