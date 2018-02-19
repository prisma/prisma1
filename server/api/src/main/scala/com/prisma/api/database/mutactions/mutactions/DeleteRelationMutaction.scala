package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.database.DatabaseMutationBuilder._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.mutations.CascadingDeletes.Path
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.{Field, Project, Relation}
import slick.dbio.DBIOAction

import scala.concurrent.Future

case class DeleteRelationMutaction(project: Project, path: Path) extends ClientSqlDataChangeMutaction {

  val relationFieldsWhereOtherSideIsRequired = path.lastModel.relationFields.filter(otherSideIsRequired)
  val extendedPaths                          = relationFieldsWhereOtherSideIsRequired.map(path.appendEdge(project, _))
  val requiredCheck                          = extendedPaths.map(oldParentFailureTriggerByPath2(project, _))

  override def execute = {
    Future.successful(ClientSqlStatementResult(DBIOAction.seq(requiredCheck: _*)))
  }

  override def handleErrors = {
    Some({
      case e: SQLException if e.getErrorCode == 1242 && otherFailingRequiredRelationOnChild(e.getCause.toString).isDefined =>
        throw RequiredRelationWouldBeViolated(project, otherFailingRequiredRelationOnChild(e.getCause.toString).get)
    })
  }

  def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] =
    extendedPaths.collectFirst { case p if causedByThisMutactionChildOnly(p, cause) => p.lastRelation_! }

  def causedByThisMutactionChildOnly(path: Path, cause: String) = {
    val parentCheckString = s"`${path.lastRelation_!.id}` OLDPARENTPATHFAILURETRIGGER WHERE `${path.lastParentSide}`"

    cause.contains(parentCheckString)
  }

  def otherSideIsRequired(field: Field): Boolean = field.relatedField(project.schema) match {
    case Some(f) if f.isRequired => true
    case Some(f)                 => false
    case None                    => false
  }
}
