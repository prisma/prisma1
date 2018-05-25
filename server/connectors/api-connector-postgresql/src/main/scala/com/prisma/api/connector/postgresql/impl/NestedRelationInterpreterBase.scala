package com.prisma.api.connector.postgresql.impl

import com.prisma.api.connector.postgresql.DatabaseMutactionInterpreter
import com.prisma.api.connector.postgresql.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.connector.{ModelEdge, NodeEdge, Path}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.{Project, Schema}
import org.postgresql.util.PSQLException
import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}

trait NestedRelationInterpreterBase extends DatabaseMutactionInterpreter {

  def path: Path
  def project: Project
  def topIsCreate: Boolean
  def schema: Schema = project.schema

  val edge              = path.lastEdge_!
  val relationTableName = path.lastRelation_!.relationTableName
  val p                 = edge.parentField
  val otherModel        = edge.child
  val otherFieldOption  = edge.childField
  val c = otherFieldOption match {
    case Some(x) => x
    case None =>
      p.template.copy(isRequired = false, isList = true).build(otherModel) // fixme: 1. obsolete magical back relation 2. passingOtherModel is not right
  }
  val parentCauseString = edge match {
    case edge: NodeEdge => s"-OLDPARENTFAILURETRIGGER@${relationTableName}@${edge.columnForChildRelationSide}@${edge.childWhere.fieldValueAsString}-"
    case _: ModelEdge   => s"-OLDPARENTFAILURETRIGGER@${relationTableName}@${edge.columnForChildRelationSide}-"
  }

  val childCauseString = path.edges.length match {
    case 0 => sys.error("There should always be at least one edge on the path if this is called.")
    case 1 => s"-OLDCHILDPATHFAILURETRIGGER@${relationTableName}@${path.lastEdge_!.columnForParentRelationSide}@${path.root.fieldValueAsString}-"
    case _ =>
      path.removeLastEdge.lastEdge_! match {
        case edge: NodeEdge => s"-OLDCHILDPATHFAILURETRIGGER@${relationTableName}@${edge.columnForParentRelationSide}@${edge.childWhere.fieldValueAsString}-"
        case _: ModelEdge   => s"-OLDCHILDPATHFAILURETRIGGER@${relationTableName}@${edge.columnForParentRelationSide}-"
      }
  }

  def checkForOldParent(implicit mb: PostgresApiDatabaseMutationBuilder) = mb.oldParentFailureTrigger(path, parentCauseString)
  def checkForOldParentByChildWhere(implicit mb: PostgresApiDatabaseMutationBuilder): slick.sql.SqlStreamingAction[Vector[String], String, slick.dbio.Effect] =
    path.lastEdge_! match {
      case _: ModelEdge   => sys.error("Should be a node edge")
      case edge: NodeEdge => mb.oldParentFailureTriggerForRequiredRelations(edge.relation, edge.childWhere, edge.childRelationSide, parentCauseString)
    }

  def checkForOldChild(implicit mb: PostgresApiDatabaseMutationBuilder) = mb.oldChildFailureTrigger(path, childCauseString)
  def noCheckRequired                                                   = List.empty

  def removalByParent(implicit mb: PostgresApiDatabaseMutationBuilder)         = mb.deleteRelationRowByParent(path)
  def removalByChildWhere(implicit mb: PostgresApiDatabaseMutationBuilder)     = mb.deleteRelationRowByChildWithWhere(path)
  def removalByParentAndChild(implicit mb: PostgresApiDatabaseMutationBuilder) = mb.deleteRelationRowByParentAndChild(path)
  def createRelationRow(implicit mb: PostgresApiDatabaseMutationBuilder)       = List(mb.createRelationRowByPath(path))
  def noActionRequired                                                         = List.empty

  def requiredCheck(implicit mb: PostgresApiDatabaseMutationBuilder): List[DBIO[_]]

  def removalActions(implicit mb: PostgresApiDatabaseMutationBuilder): List[DBIO[_]]

  def addAction(implicit mb: PostgresApiDatabaseMutationBuilder): List[DBIO[_]]

  def allActions(implicit mb: PostgresApiDatabaseMutationBuilder) = requiredCheck ++ removalActions ++ addAction

  override def action(mb: PostgresApiDatabaseMutationBuilder) = DBIOAction.seq(allActions(mb): _*)

  override val errorMapper = {
    case e: PSQLException if causedByThisMutaction(e.getMessage) => throw RequiredRelationWouldBeViolated(project, path.lastRelation_!)
  }

  def requiredRelationViolation = throw RequiredRelationWouldBeViolated(project, path.lastRelation_!)

  def sysError = sys.error("This should not happen, since it means a many side is required")

  def causedByThisMutaction(cause: String) = cause.contains(parentCauseString) || cause.contains(childCauseString)

}
