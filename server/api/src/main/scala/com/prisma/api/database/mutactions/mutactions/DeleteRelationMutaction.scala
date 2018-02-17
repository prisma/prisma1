package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.database.DatabaseMutationBuilder._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.NodeSelector
import com.prisma.api.mutations.mutations.CascadingDeletes.{ModelEdge, NodeEdge, Path}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.{Field, Project, Relation}
import com.prisma.util.gc_value.OtherGCStuff.parameterStringFromSQLException
import slick.dbio.DBIOAction

import scala.concurrent.Future

case class DeleteRelationMutaction(project: Project, path: Path) extends ClientSqlDataChangeMutaction {

  val relationFieldsWhereOtherSideIsRequired = path.lastModel.relationFields.filter(otherSideIsRequired)

  val requiredCheck = path match {
    case _ if path.edges.nonEmpty && path.lastEdge_!.isInstanceOf[ModelEdge] =>
      List.empty //todo
    case _ if path.edges.nonEmpty && path.lastEdge_!.isInstanceOf[NodeEdge] =>
      relationFieldsWhereOtherSideIsRequired.map(field =>
        oldParentFailureTriggerForRequiredRelations(project, field.relation.get, path.lastEdge_!.asInstanceOf[NodeEdge].childWhere, field.relationSide.get))
    //todo
    case _ =>
      relationFieldsWhereOtherSideIsRequired.map(field =>
        oldParentFailureTriggerForRequiredRelations(project, field.relation.get, path.root, field.relationSide.get))

  }

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
    relationFieldsWhereOtherSideIsRequired.collectFirst { case f if causedByThisMutactionChildOnly(f, f.relation.get, cause) => f.relation.get }

  def causedByThisMutactionChildOnly(field: Field, relation: Relation, cause: String) = {
    val parentCheckString = s"`${project.id}`.`${relation.id}` OLDPARENTFAILURETRIGGER WHERE `${field.relationSide.get}`"

    cause.contains(parentCheckString) //&& cause.contains(parameterStringFromSQLException(where)) todo
  }

  def otherSideIsRequired(field: Field): Boolean = field.relatedField(project.schema) match {
    case Some(f) if f.isRequired => true
    case Some(f)                 => false
    case None                    => false
  }
}
