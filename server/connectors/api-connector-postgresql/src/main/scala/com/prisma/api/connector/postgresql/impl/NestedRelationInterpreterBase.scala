package com.prisma.api.connector.postgresql.impl

import com.prisma.api.connector.postgresql.DatabaseMutactionInterpreter
import com.prisma.api.connector.postgresql.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.connector.{ModelEdge, NodeEdge, Path}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.Project
import org.postgresql.util.PSQLException
import slick.dbio.{DBIOAction, Effect, NoStream}

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

  def checkForOldParent(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder) = mutationBuilder.oldParentFailureTrigger(path, parentCauseString)
  def checkForOldParentByChildWhere(
      implicit mutationBuilder: PostgresApiDatabaseMutationBuilder): slick.sql.SqlStreamingAction[Vector[String], String, slick.dbio.Effect] =
    path.lastEdge_! match {
      case _: ModelEdge => sys.error("Should be a node edge")
      case edge: NodeEdge =>
        mutationBuilder.oldParentFailureTriggerForRequiredRelations(edge.relation, edge.childWhere, edge.childRelationSide, parentCauseString)
    }

  def checkForOldChild(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder) = mutationBuilder.oldChildFailureTrigger(path, childCauseString)
  def noCheckRequired                                                                = List.empty

  def removalByParent(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder)         = mutationBuilder.deleteRelationRowByParent(path)
  def removalByChildWhere(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder)     = mutationBuilder.deleteRelationRowByChildWithWhere(path)
  def removalByParentAndChild(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder) = mutationBuilder.deleteRelationRowByParentAndChild(path)
  def createRelationRow(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder)       = List(mutationBuilder.createRelationRowByPath(project.schema, path))
  def noActionRequired                                                                      = List.empty

  def requiredCheck(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder): List[DBIOAction[_, NoStream, Effect]]

  def removalActions(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder): List[DBIOAction[_, NoStream, Effect]]

  def addAction(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder): List[DBIOAction[_, NoStream, Effect]]

  def allActions(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder) = requiredCheck ++ removalActions ++ addAction

  override def action(mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
    DBIOAction.seq(allActions(mutationBuilder): _*)
  }

  override val errorMapper = {
    case e: PSQLException if causedByThisMutaction(e.getMessage) => throw RequiredRelationWouldBeViolated(project, path.lastRelation_!)
  }

  def requiredRelationViolation = throw RequiredRelationWouldBeViolated(project, path.lastRelation_!)

  def sysError = sys.error("This should not happen, since it means a many side is required")

  def causedByThisMutaction(cause: String) = cause.contains(parentCauseString) || cause.contains(childCauseString)

}
