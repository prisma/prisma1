package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.database._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.mutations.CascadingDeletes.{ModelEdge, NodeEdge, Path}
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.Project
import com.prisma.util.gc_value.OtherGCStuff.parameterStringFromSQLException

import scala.concurrent.Future

case class VerifyConnection(project: Project, path: Path) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.connectionFailureTriggerPath(project, path)))
  }

  override def handleErrors = {
    Some({
      case e: SQLException if e.getErrorCode == 1242 && causedByThisMutaction(e.getCause.toString) => throw APIErrors.NodesNotConnectedError(path)
    })
  }

  def causedByThisMutaction(cause: String) = {
    val childString  = s"`${path.lastRelation_!.id}` CONNECTIONFAILURETRIGGER WHERE `${path.lastEdge_!.childRelationSide}`"
    val parentString = s"AND `${path.lastEdge_!.parentRelationSide}`"

    path.lastEdge_! match {
      case edge: ModelEdge => cause.contains(childString) && cause.contains(parentString)
      case edge: NodeEdge  => cause.contains(childString) && cause.contains(parentString) && cause.contains(parameterStringFromSQLException(edge.childWhere))
    }
  }
}
