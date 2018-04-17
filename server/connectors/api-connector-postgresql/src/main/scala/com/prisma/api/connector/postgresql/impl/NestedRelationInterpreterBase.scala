package com.prisma.api.connector.postgresql.impl

import java.sql.SQLException

import com.prisma.api.connector.postgresql.DatabaseMutactionInterpreter
import com.prisma.api.connector.{ModelEdge, NodeEdge, Path}
import com.prisma.api.connector.postgresql.database.DatabaseMutationBuilder._
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.Project
import slick.dbio.{DBIOAction, Effect, NoStream}
import com.prisma.api.connector.postgresql.database.ErrorMessageParameterHelper.parameterString
import org.postgresql.util.PSQLException

trait NestedRelationInterpreterBase extends DatabaseMutactionInterpreter {

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

  val parentCauseString = path.lastEdge_! match {
    case edge: NodeEdge =>
      s"-OLDPARENTFAILURETRIGGER@${path.lastRelation_!.relationTableName}@${path.lastEdge_!.childRelationSide}@${edge.childWhere.fieldValueAsString}-"
    case _: ModelEdge => s"-OLDPARENTFAILURETRIGGER@${path.lastRelation_!.relationTableName}@${path.lastEdge_!.childRelationSide}-"
  }

  val childCauseString = path.edges.length match {
    case 0 => sys.error("There should always be at least one edge on the path if this is called.")
    case 1 => s"-OLDCHILDPATHFAILURETRIGGER@${path.lastRelation_!.relationTableName}@${path.lastEdge_!.parentRelationSide}@${path.root.fieldValueAsString}-"
    case _ =>
      path.removeLastEdge.lastEdge_! match {
        case edge: NodeEdge =>
          s"-OLDCHILDPATHFAILURETRIGGER@${path.lastRelation_!.relationTableName}@${path.lastEdge_!.parentRelationSide}@${edge.childWhere.fieldValueAsString}-"
        case _: ModelEdge => s"-OLDCHILDPATHFAILURETRIGGER@${path.lastRelation_!.relationTableName}@${path.lastEdge_!.parentRelationSide}-"
      }
  }

  def checkForOldParent = oldParentFailureTrigger(project, path, parentCauseString)
  def checkForOldParentByChildWhere: slick.sql.SqlStreamingAction[Vector[String], String, slick.dbio.Effect] = path.lastEdge_! match {
    case _: ModelEdge   => sys.error("Should be a node edge")
    case edge: NodeEdge => oldParentFailureTriggerForRequiredRelations(project, edge.relation, edge.childWhere, edge.childRelationSide, parentCauseString)
  }

  def checkForOldChild = oldChildFailureTrigger(project, path, childCauseString)
  def noCheckRequired  = List.empty

  def removalByParent         = deleteRelationRowByParent(project.id, path)
  def removalByChildWhere     = deleteRelationRowByChildWithWhere(project.id, path)
  def removalByParentAndChild = deleteRelationRowByParentAndChild(project.id, path)
  def createRelationRow       = List(createRelationRowByPath(project.id, path))
  def noActionRequired        = List.empty

  def requiredCheck: List[DBIOAction[_, NoStream, Effect]]

  def removalActions: List[DBIOAction[_, NoStream, Effect]]

  def addAction: List[DBIOAction[_, NoStream, Effect]]

  def allActions = requiredCheck ++ removalActions ++ addAction

  override val action = DBIOAction.seq(allActions: _*)

  override val errorMapper = {
    case e: PSQLException if causedByThisMutaction(e.getMessage) => throw RequiredRelationWouldBeViolated(project, path.lastRelation_!)
  }

  def requiredRelationViolation = throw RequiredRelationWouldBeViolated(project, path.lastRelation_!)

  def sysError = sys.error("This should not happen, since it means a many side is required")

  def causedByThisMutaction(cause: String) = cause.contains(parentCauseString) || cause.contains(childCauseString)

}
