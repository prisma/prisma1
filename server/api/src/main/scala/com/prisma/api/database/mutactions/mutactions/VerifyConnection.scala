package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.database._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.mutations.CascadingDeletes.{ModelEdge, NodeEdge, Path}
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.Project
import com.prisma.util.gc_value.OtherGCStuff.parameterString

import scala.concurrent.Future

case class VerifyConnection(project: Project, path: Path) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.connectionFailureTrigger(project, path)))
  }

  override def handleErrors = {
    Some({
      case e: SQLException if e.getErrorCode == 1242 && causedByThisMutaction(e.getCause.toString) => throw APIErrors.NodesNotConnectedError(path)
    })
  }

  def causedByThisMutaction(cause: String) = {
    val string = s"`${path.lastRelation_!.id}` CONNECTIONFAILURETRIGGERPATH WHERE "

    path.lastEdge_! match {
      case _: ModelEdge   => cause.contains(string ++ s"`${path.lastParentSide}`")
      case edge: NodeEdge => cause.contains(string ++ s"`${path.lastChildSide}`") && cause.contains(parameterString(edge.childWhere))
    }
  }
}
