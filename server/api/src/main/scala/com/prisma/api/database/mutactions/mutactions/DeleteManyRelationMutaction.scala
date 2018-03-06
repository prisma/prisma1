package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.database.DatabaseMutationBuilder._
import com.prisma.api.database.Types.DataItemFilterCollection
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.{Field, Model, Project, Relation}
import slick.dbio.DBIOAction

import scala.concurrent.Future

case class DeleteManyRelationMutaction(project: Project, model: Model, filter: DataItemFilterCollection) extends ClientSqlDataChangeMutaction {

  val fieldsWhereThisModelIsRequired =
    project.schema.allFields.filter(f => f.isRequired && !f.isList && f.relatedModel(project.schema).contains(model))
  val requiredCheck = fieldsWhereThisModelIsRequired.map(oldParentFailureTriggerByFieldAndFilter(project, model, filter, _))

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
    val parentCheckString = s"`${field.relation.get.id}` OLDPARENTPATHFAILURETRIGGERBYFIELDANDFILTER WHERE `${field.oppositeRelationSide.get}`"
    cause.contains(parentCheckString) //todo add filter
  }
}
