package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.database.DatabaseMutationBuilder._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.{NodeSelector, ParentInfo}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.{Project, Relation}
import com.prisma.util.gc_value.OtherGCStuff.parameterStringFromSQLException
import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.concurrent.Future

trait NestedRelationMutactionBaseClass extends ClientSqlDataChangeMutaction {

  def project: Project
  def parentInfo: ParentInfo
  def where: NodeSelector
  def topIsCreate: Boolean

  val p                = parentInfo.field
  val otherModel       = parentInfo.relation.getOtherModel_!(project.schema, parentInfo.where.model)
  val otherFieldOption = otherModel.fields.find(_.relation.contains(parentInfo.relation))
  val c = otherFieldOption match {
    case Some(x) => x
    case None    => p.copy(isRequired = false, isList = true) //optional back-relation defaults to List-NonRequired
  }

  def checkForOldParent = oldParentFailureTriggerForRequiredRelations(project, parentInfo.relation, where, parentInfo.field.oppositeRelationSide.get)
  def checkForOldChild  = oldChildFailureTriggerForRequiredRelations(project, parentInfo)
  def noCheckRequired   = List.empty

  def removalByParent         = deleteRelationRowByParent(project.id, parentInfo)
  def removalByChild          = deleteRelationRowByChild(project.id, parentInfo, where)
  def removalByParentAndChild = deleteRelationRowByParentAndChild(project.id, parentInfo, where)
  def createRelationRow       = List(createRelationRowByUniqueValueForChild(project.id, parentInfo, where))
  def noActionRequired        = List.empty

  def requiredCheck: List[DBIOAction[_, NoStream, Effect]]

  def removalActions: List[DBIOAction[_, NoStream, Effect]]

  def addAction: List[DBIOAction[_, NoStream, Effect]]

  def allActions = requiredCheck ++ removalActions ++ addAction

  override def execute = {
    Future.successful(ClientSqlStatementResult(DBIOAction.seq(allActions: _*)))
  }

  override def handleErrors = {
    Some({
      case e: SQLException if e.getErrorCode == 1242 && causedByThisMutaction(parentInfo, e.getCause.toString) =>
        throw RequiredRelationWouldBeViolated(project, parentInfo.relation)
    })
  }

  def requiredRelationViolation = throw RequiredRelationWouldBeViolated(project, parentInfo.relation)

  def sysError = sys.error("This should not happen, since it means a many side is required")

  def causedByThisMutaction(parentInfo: ParentInfo, cause: String) = {
    val parentCheckString = s"`${parentInfo.relation.id}` OLDPARENTFAILURETRIGGER WHERE `${parentInfo.field.oppositeRelationSide.get}`"
    val childCheckString  = s"`${parentInfo.relation.id}` OLDCHILDFAILURETRIGGER WHERE `${parentInfo.field.relationSide.get}`"

    (cause.contains(parentCheckString) && cause.contains(parameterStringFromSQLException(where))) ||
    (cause.contains(childCheckString) && cause.contains(parameterStringFromSQLException(parentInfo.where)))
  }
}
