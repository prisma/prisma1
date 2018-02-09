package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.database._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.{NodeSelector, ParentInfo}
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.Project
import com.prisma.util.gc_value.OtherGCStuff.parameterStringFromSQLException

import scala.concurrent.Future

case class VerifyConnection(project: Project, parentInfo: ParentInfo, where: NodeSelector) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.connectionFailureTrigger(project, parentInfo, where)))
  }

  override def handleErrors = {
    Some({
      case e: SQLException if e.getErrorCode == 1242 && causedByThisMutaction(e.getCause.toString) => throw APIErrors.NodesNotConnectedError(parentInfo, where)
    })
  }

  def causedByThisMutaction(cause: String) = {
    val childString  = s"`${parentInfo.relation.id}` CONNECTIONFAILURETRIGGER WHERE `${parentInfo.relation.sideOf(where.model)}`"
    val parentString = s"AND `${parentInfo.relation.sideOf(parentInfo.where.model)}`"

    cause.contains(childString) && cause.contains(parentString) && cause.contains(parameterStringFromSQLException(where))
  }
}
