package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.database.DatabaseMutationBuilder._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.mutations.CascadingDeletes.{ModelEdge, NodeEdge, Path}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.{Project, Relation}
import com.prisma.util.gc_value.OtherGCStuff.parameterString
import slick.dbio.DBIOAction

import scala.concurrent.Future

case class CascadingDeleteRelationMutactions(project: Project, path: Path) extends ClientSqlDataChangeMutaction {

  val relationFieldsNotOnPath         = path.lastModel.relationFields.filter(f => !path.edges.map(_.relation).contains(f.relation.get))
  val requiredRelationFieldsNotOnPath = relationFieldsNotOnPath.filter(_.otherSideIsRequired(project))
  val extendedPaths                   = requiredRelationFieldsNotOnPath.map(path.appendEdge(project, _))

  val requiredCheck = extendedPaths.map(oldParentFailureTrigger(project, _))

  val deleteAction = List(cascadingDeleteChildActions(project.id, path))

  override def execute = {
    val allActions = requiredCheck ++ deleteAction
    Future.successful(ClientSqlStatementResult(DBIOAction.seq(allActions: _*)))
  }

  override def handleErrors = {
    Some({
      case e: SQLException if e.getErrorCode == 1242 && otherFailingRequiredRelationOnChild(e.getCause.toString).isDefined =>
        throw RequiredRelationWouldBeViolated(project, otherFailingRequiredRelationOnChild(e.getCause.toString).get)
    })
  }

  private def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] = {
    extendedPaths.collectFirst { case x: Path if causedByThisMutactionChildOnly(x, cause) => x.lastRelation_! }
  }

  private def causedByThisMutactionChildOnly(path: Path, cause: String) = {
    val parentCheckString = s"`${path.lastRelation_!.id}` OLDPARENTPATHFAILURETRIGGER WHERE `${path.lastChildSide}`"

    path.lastEdge_! match {
      case edge: NodeEdge => cause.contains(parentCheckString) && cause.contains(parameterString(edge.childWhere))
      case _: ModelEdge   => cause.contains(parentCheckString)
    }
  }
}
