package com.prisma.deploy.migration.mutactions

import com.prisma.deploy.database.DatabaseMutationBuilder

import scala.concurrent.Future
import slick.jdbc.MySQLProfile.api._

case class RenameTable(projectId: String, previousName: String, nextName: String, scalarListFieldsNames: Vector[String]) extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = setName(previousName, nextName)

  override def rollback = Some(setName(nextName, previousName))

  private def setName(previousName: String, nextName: String): Future[ClientSqlStatementResult[Any]] = Future.successful {
    val changeModelTableName = DatabaseMutationBuilder.renameTable(projectId = projectId, name = previousName, newName = nextName)
    val changeScalarListFieldTableNames =
      scalarListFieldsNames.map(fieldName => DatabaseMutationBuilder.renameScalarListTable(projectId, previousName, fieldName, nextName, fieldName))

    ClientSqlStatementResult(sqlAction = DBIO.seq(changeScalarListFieldTableNames :+ changeModelTableName: _*))
  }
}
