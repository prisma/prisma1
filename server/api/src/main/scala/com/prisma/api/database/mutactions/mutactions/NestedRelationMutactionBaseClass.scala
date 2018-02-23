package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.database.DatabaseMutationBuilder._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.mutations.CascadingDeletes.{ModelEdge, NodeEdge, Path}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.Project
import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.concurrent.Future

trait NestedRelationMutactionBaseClass extends ClientSqlDataChangeMutaction {

  def path: Path
  def project: Project
  def topIsCreate: Boolean

  val lastEdge         = path.lastEdge_!
  val p                = lastEdge.parentField
  val otherModel       = lastEdge.child
  val otherFieldOption = lastEdge.childField
  val c = otherFieldOption match {
    case Some(x) => x
    case None    => p.copy(isRequired = false, isList = true) //optional back-relation defaults to List-NonRequired
  }

  def checkForOldParent = oldParentFailureTriggerByPath(project, path)
  def checkForOldParentByChildWhere = path.lastEdge_! match {
    case _: ModelEdge   => sys.error("Should be a node edge")
    case edge: NodeEdge => oldParentFailureTriggerForRequiredRelations(project, edge.relation, edge.childWhere, edge.childRelationSide)
  }

  def checkForOldChild = oldChildFailureTriggerByPath(project, path)
  def noCheckRequired  = List.empty

  def removalByParent         = deleteRelationRowByParentPath(project.id, path)
  def removalByChildWhere     = deleteRelationRowByChildPathWithWhere(project.id, path)
  def removalByParentAndChild = deleteRelationRowByParentAndChildPath(project.id, path)
  def createRelationRow       = List(createRelationRowByPath(project.id, path))
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
      case e: SQLException if e.getErrorCode == 1242 && causedByThisMutaction(path, e.getCause.toString) =>
        throw RequiredRelationWouldBeViolated(project, path.lastRelation_!)
    })
  }

  def requiredRelationViolation = throw RequiredRelationWouldBeViolated(project, path.lastRelation_!)

  def sysError = sys.error("This should not happen, since it means a many side is required")

  def causedByThisMutaction(path: Path, cause: String) = {
    val parentCheckString = s"`${path.lastRelation_!.id}` OLDPARENTFAILURETRIGGER WHERE `${path.lastEdge_!.childRelationSide}`"
    val childCheckString  = s"`${path.lastRelation_!.id}` OLDCHILDFAILURETRIGGER WHERE `${path.lastEdge_!.parentRelationSide}`"

//    (cause.contains(parentCheckString) && cause.contains(parameterStringFromSQLException(where))) ||  //todo reintroduce check on parameter
//    (cause.contains(childCheckString) && cause.contains(parameterStringFromSQLException(parentInfo.where)))

    cause.contains(parentCheckString) || cause.contains(childCheckString)
  }
}
