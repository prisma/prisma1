package com.prisma.deploy.migration.mutactions

import com.prisma.deploy.database.DatabaseMutationBuilder
import com.prisma.shared.models.{Field, Model}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class UpdateScalarListTable(projectId: String, oldModel: Model, newModel: Model, oldField: Field, newField: Field) extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {

    val updateType = if (oldField.typeIdentifier != newField.typeIdentifier) {
      List(DatabaseMutationBuilder.updateScalarListType(projectId, oldModel.name, oldField.name, newField.typeIdentifier))
    } else {
      List.empty
    }

    val renameTable = if (oldField.name != newField.name || oldModel.name != newModel.name) {
      List(DatabaseMutationBuilder.renameScalarListTable(projectId, oldModel.name, oldField.name, newModel.name, newField.name))
    } else {
      List.empty
    }

    val changes = updateType ++ renameTable

    if (changes.isEmpty) {
      Future.successful(ClientSqlStatementResult(sqlAction = DBIO.successful(())))
    } else {
      Future.successful(ClientSqlStatementResult(sqlAction = DBIO.seq(changes: _*)))
    }

  }

  override def rollback: Some[Future[ClientSqlStatementResult[Any]]] = Some(UpdateScalarListTable(projectId, newModel, oldModel, newField, oldField).execute)
}
