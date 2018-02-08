package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.database.DatabaseMutationBuilder._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.NodeSelector
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.{Field, Project, Relation}
import com.prisma.util.gc_value.OtherGCStuff.parameterStringFromSQLException
import slick.dbio.DBIOAction

import scala.concurrent.Future

case class DeleteRelationMutaction(project: Project, where: NodeSelector) extends ClientSqlDataChangeMutaction {

  val relationsWhereThisIsRequired = where.model.relationFields.filter(otherSideIsRequired).map(_.relation.get)

  val requiredCheck = relationsWhereThisIsRequired.map(relation => oldParentFailureTriggerForRequiredRelations(project, relation, where))

  override def execute = {
    Future.successful(ClientSqlStatementResult(DBIOAction.seq(requiredCheck: _*)))
  }

  override def handleErrors = {
    Some({
      case e: SQLException if e.getErrorCode == 1242 && otherFailingRequiredRelationOnChild(e.getCause.toString).isDefined =>
        throw RequiredRelationWouldBeViolated(project, otherFailingRequiredRelationOnChild(e.getCause.toString).get)
    })
  }

  def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] = relationsWhereThisIsRequired.collectFirst {
    case x if causedByThisMutactionChildOnly(x, cause) => x
  }

  def causedByThisMutactionChildOnly(relation: Relation, cause: String) = {
    val parentCheckString = s"`${project.id}`.`${relation.id}` OLDPARENTFAILURETRIGGER WHERE `${relation.sideOf(where.model)}`"

    cause.contains(parentCheckString) && cause.contains(parameterStringFromSQLException(where))
  }

  def otherSideIsRequired(field: Field): Boolean = field.relatedField(project.schema) match {
    case Some(f) if f.isRequired => true
    case Some(f)                 => false
    case None                    => false
  }
}
