package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.connector.NodeSelector
import com.prisma.api.database._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.Project
import com.prisma.util.gc_value.OtherGCStuff.parameterString

import scala.concurrent.Future

case class VerifyWhere(project: Project, where: NodeSelector) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.whereFailureTrigger(project, where)))
  }

  override def handleErrors = {
    Some({ case e: SQLException if e.getErrorCode == 1242 && causedByThisMutaction(e.getCause.toString) => throw APIErrors.NodeNotFoundForWhereError(where) })
  }

  def causedByThisMutaction(cause: String) = {
    val modelString = s"`${where.model.name}` WHEREFAILURETRIGGER WHERE `${where.field.name}`"
    cause.contains(modelString) && cause.contains(parameterString(where))
  }
}
