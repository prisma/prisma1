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

case class CascadingDeleteRelationMutactions(project: Project, path: Path) extends ClientSqlDataChangeMutaction {
  val fieldsWhereThisModelIsRequired =
    project.schema.allFields.filter(f => f.isRequired && !f.isList && f.relatedModel(project.schema).contains(path.lastModel))

  val otherFieldsWhereThisModelIsRequired = path.lastEdge match {
    case Some(edge) => fieldsWhereThisModelIsRequired.filter(f => f != edge.parentField)
    case None       => fieldsWhereThisModelIsRequired
  }

  val requiredCheck = otherFieldsWhereThisModelIsRequired.map(oldParentFailureTriggerByField(project, path, _))

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

  private def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] =
    otherFieldsWhereThisModelIsRequired.collectFirst { case f if causedByThisMutactionChildOnly(f, cause) => f.relation.get }

  private def causedByThisMutactionChildOnly(field: Field, cause: String) = {
    val parentCheckString = s"`${field.relation.get.id}` OLDPARENTPATHFAILURETRIGGERBYFIELD WHERE `${field.oppositeRelationSide.get}`"

    path.lastEdge match {
      case Some(edge: NodeEdge) => cause.contains(parentCheckString) && cause.contains(parameterString(edge.childWhere))
      case _                    => cause.contains(parentCheckString)
    }
  }
}
