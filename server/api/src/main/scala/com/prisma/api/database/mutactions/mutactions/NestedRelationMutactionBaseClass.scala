package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.database.DatabaseMutationBuilder._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.{NodeSelector, ParentInfo}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.gc_values._
import com.prisma.shared.models.{Project, Relation}
import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.concurrent.Future

trait NestedRelationMutactionBaseClass extends ClientSqlDataChangeMutaction {

  def project: Project
  def parentInfo: ParentInfo
  def where: NodeSelector
  def topIsCreate: Boolean

  val p = parentInfo.field
  val c = parentInfo.relation.getOtherModel_!(project.schema, parentInfo.where.model).fields.find(_.relation.contains(parentInfo.relation)).get

  val checkForOldParent = oldParentFailureTriggerForRequiredRelations(project, parentInfo.relation, where)
  val checkForOldChild  = oldChildFailureTriggerForRequiredRelations(project, parentInfo)
  val noCheckRequired   = List.empty

  val removalByParent         = deleteRelationRowByParent(project.id, parentInfo)
  val removalByChild          = deleteRelationRowByChild(project.id, parentInfo.relation, where)
  val removalByParentAndChild = deleteRelationRowByParentAndChild(project.id, parentInfo, where)
  val createRelationRow       = List(createRelationRowByUniqueValueForChild(project.id, parentInfo, where))
  val noActionRequired        = List.empty

  def requiredCheck: List[DBIOAction[_, NoStream, Effect]]

  def removalActions: List[DBIOAction[_, NoStream, Effect]]

  def addAction: List[DBIOAction[_, NoStream, Effect]]

  override def execute = {
    val allActions = requiredCheck ++ removalActions ++ addAction
    Future.successful(ClientSqlStatementResult(DBIOAction.seq(allActions: _*)))
  }

  override def handleErrors = {
    Some({
      case e: SQLException if e.getErrorCode == 1242 && causedByThisMutaction(parentInfo.relation, e.getCause.toString) =>
        throw RequiredRelationWouldBeViolated(project, parentInfo.relation)
    })
  }

  def requiredRelationViolation = throw RequiredRelationWouldBeViolated(project, parentInfo.relation)
  def sysError                  = sys.error("This should not happen, since it means a many side is required")

  def causedByThisMutaction(relation: Relation, cause: String) = {
    val parentCheckString = s"`${relation.id}` where `${relation.sideOf(where.model)}` ="
    val childCheckString  = s"`${relation.id}` where `${relation.sideOf(parentInfo.model)}` ="

    (cause.contains(parentCheckString) && cause.contains(parameterString(where))) ||
    (cause.contains(childCheckString) && cause.contains(parameterString(parentInfo.where)))
  }

  def causedByThisMutactionChildOnly(relation: Relation, cause: String) = {
    val parentCheckString = s"`${relation.id}` where `${relation.sideOf(where.model)}` ="

    cause.contains(parentCheckString) && cause.contains(parameterString(where))
  }

  def parameterString(where: NodeSelector) = where.fieldValue match {
    case StringGCValue(x)      => s"parameters ['$x',"
    case IntGCValue(x)         => s"parameters [$x,"
    case FloatGCValue(x)       => s"parameters [$x,"
    case BooleanGCValue(false) => s"parameters [0,"
    case BooleanGCValue(true)  => s"parameters [1,"
    case GraphQLIdGCValue(x)   => s"parameters ['$x',"
    case EnumGCValue(x)        => s"parameters ['$x',"
    case DateTimeGCValue(x)    => throw sys.error("Implement DateTime") // todo
    case JsonGCValue(x)        => s"parameters ['$x',"
    case ListGCValue(_)        => sys.error("Not an acceptable Where")
    case RootGCValue(_)        => sys.error("Not an acceptable Where")
    case NullGCValue           => sys.error("Not an acceptable Where")
  }
}
