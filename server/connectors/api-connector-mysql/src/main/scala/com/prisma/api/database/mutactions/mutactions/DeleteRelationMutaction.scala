package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.connector.{NodeEdge, Path}
import com.prisma.api.database.DatabaseMutationBuilder._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.{Field, Project, Relation}
import com.prisma.util.gc_value.OtherGCStuff.parameterString
import slick.dbio.DBIOAction

import scala.concurrent.Future

case class DeleteRelationMutaction(project: Project, path: Path) extends ClientSqlDataChangeMutaction {

  val fieldsWhereThisModelIsRequired =
    project.schema.allFields.filter(f => f.isRequired && !f.isList && f.relatedModel(project.schema).contains(path.lastModel))
  val requiredCheck = fieldsWhereThisModelIsRequired.map(oldParentFailureTriggerByField(project, path, _))

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
    fieldsWhereThisModelIsRequired.collectFirst { case f if causedByThisMutactionChildOnly(f, cause) => f.relation.get }

  private def causedByThisMutactionChildOnly(field: Field, cause: String) = {
    val parentCheckString = s"`${field.relation.get.id}` OLDPARENTPATHFAILURETRIGGERBYFIELD WHERE `${field.oppositeRelationSide.get}`"

    path.lastEdge match {
      case Some(edge: NodeEdge) => cause.contains(parentCheckString) && cause.contains(parameterString(edge.childWhere))
      case _                    => cause.contains(parentCheckString)
    }
  }
}
