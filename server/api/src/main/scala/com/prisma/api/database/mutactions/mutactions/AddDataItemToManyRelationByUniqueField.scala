package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.{NodeSelector, ParentInfo}
import com.prisma.shared.models._

import scala.concurrent.Future

case class AddDataItemToManyRelationByUniqueField(project: Project, parentInfo: ParentInfo, where: NodeSelector) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {

    ClientSqlStatementResult(DatabaseMutationBuilder.createRelationRowByUniqueValueForChild(project.id, parentInfo, where))
  }
}
