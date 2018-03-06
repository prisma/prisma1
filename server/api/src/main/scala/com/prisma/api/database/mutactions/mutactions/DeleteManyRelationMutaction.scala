package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.database.DatabaseMutationBuilder._
import com.prisma.api.database.Types.DataItemFilterCollection
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.mutations.CascadingDeletes.{ModelEdge, NodeEdge, Path}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.{Model, Project, Relation}
import com.prisma.util.gc_value.OtherGCStuff.parameterString
import slick.dbio.DBIOAction

import scala.concurrent.Future

case class DeleteManyRelationMutaction(project: Project, model: Model, filter: DataItemFilterCollection) extends ClientSqlDataChangeMutaction {

  val relationFieldsWhereOtherSideIsRequired = model.relationFields.filter(_.otherSideIsRequired(project)) //todo use new logic
  val extendedPaths                          = relationFieldsWhereOtherSideIsRequired.map(path.appendEdge(project, _))
  val requiredCheck                          = extendedPaths.map(oldChildFailureTrigger(project, _))

  override def execute = {
    Future.successful(ClientSqlStatementResult(DBIOAction.seq(requiredCheck: _*)))
  }

  override def handleErrors = {
    Some({
      case e: SQLException if e.getErrorCode == 1242 && otherFailingRequiredRelationOnChild(e.getCause.toString).isDefined =>
        throw RequiredRelationWouldBeViolated(project, otherFailingRequiredRelationOnChild(e.getCause.toString).get)
    })
  }

  private def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] =
    extendedPaths.collectFirst { case p if causedByThisMutactionChildOnly(p, cause) => p.lastRelation_! }

  private def causedByThisMutactionChildOnly(path: Path, cause: String) = {
    val parentCheckString = s"`${path.lastRelation_!.id}` OLDCHILDPATHFAILURETRIGGER WHERE `${path.parentSideOfLastEdge}`"

    path.lastEdge_! match {
      case edge: NodeEdge => cause.contains(parentCheckString) && cause.contains(parameterString(edge.childWhere))
      case _: ModelEdge   => cause.contains(parentCheckString)
    }
  }
}
