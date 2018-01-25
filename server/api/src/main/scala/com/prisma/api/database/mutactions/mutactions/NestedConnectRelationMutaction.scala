package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException
import com.prisma.api.database.DatabaseMutationBuilder._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.{NodeSelector, ParentInfo}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.gc_values._
import com.prisma.shared.models.Project
import slick.dbio.DBIOAction
import scala.concurrent.Future

case class NestedConnectRelationMutaction(project: Project, parentInfo: ParentInfo, where: NodeSelector, topIsCreate: Boolean)
    extends ClientSqlDataChangeMutaction {

  override def execute = {

    val p = parentInfo.field
    val c = parentInfo.relation.getOtherModel_!(project.schema, parentInfo.where.model).fields.find(_.relation.contains(parentInfo.relation)).get

    val checkForOldParent = oldParentFailureTriggerForRequiredRelations(project, parentInfo, where)
    val checkForOldChild  = oldChildFailureTriggerForRequiredRelations(project, parentInfo)

    val removalByParent         = deleteRelationRowByParent(project.id, parentInfo)
    val removalByChild          = deleteRelationRowByChild(project.id, parentInfo, where)
    val removalByParentAndChild = deleteRelationRowByParentAndChild(project.id, parentInfo, where)

    val requiredCheck = topIsCreate match {
      case false =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => throw RequiredRelationWouldBeViolated(parentInfo, where)
          case (false, true, false, false)  => List(checkForOldParent)
          case (false, false, false, true)  => List(checkForOldChild)
          case (false, false, false, false) => List.empty
          case (true, false, false, true)   => List.empty
          case (true, false, false, false)  => List.empty
          case (false, true, true, false)   => List.empty
          case (false, false, true, false)  => List.empty
          case (true, false, true, false)   => List.empty
          case _                            => sys.error("This should not happen, since it means a many side is required")
        }
      case true =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => throw RequiredRelationWouldBeViolated(parentInfo, where)
          case (false, true, false, false)  => List(checkForOldParent)
          case (false, false, false, true)  => List.empty
          case (false, false, false, false) => List.empty
          case (true, false, false, true)   => List.empty
          case (true, false, false, false)  => List.empty
          case (false, true, true, false)   => List.empty
          case (false, false, true, false)  => List.empty
          case (true, false, true, false)   => List.empty
          case _                            => sys.error("This should not happen, since it means a many side is required")
        }
    }

    val removalActions = topIsCreate match {
      case false =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => throw RequiredRelationWouldBeViolated(parentInfo, where)
          case (false, true, false, false)  => List(removalByParent)
          case (false, false, false, true)  => List(removalByParent, removalByChild)
          case (false, false, false, false) => List(removalByParent, removalByChild)
          case (true, false, false, true)   => List(removalByChild)
          case (true, false, false, false)  => List(removalByChild)
          case (false, true, true, false)   => List(removalByParent)
          case (false, false, true, false)  => List(removalByParent)
          case (true, false, true, false)   => List.empty
          case _                            => sys.error("This should not happen, since it means a many side is required")
        }
      case true =>
        (p.isList, p.isRequired, c.isList, c.isRequired) match {
          case (false, true, false, true)   => throw RequiredRelationWouldBeViolated(parentInfo, where)
          case (false, true, false, false)  => List.empty
          case (false, false, false, true)  => List(removalByChild)
          case (false, false, false, false) => List(removalByChild)
          case (true, false, false, true)   => List(removalByChild)
          case (true, false, false, false)  => List(removalByChild)
          case (false, true, true, false)   => List.empty
          case (false, false, true, false)  => List.empty
          case (true, false, true, false)   => List.empty
          case _                            => sys.error("This should not happen, since it means a many side is required")
        }
    }

    val addAction = createRelationRowByUniqueValueForChild(project.id, parentInfo, where)

    val allActions = requiredCheck ++ removalActions :+ addAction

    Future.successful(ClientSqlStatementResult(DBIOAction.seq(allActions: _*)))
  }

  override def handleErrors = {
    Some({
      case e: SQLException if e.getErrorCode == 1242 && causedByThisMutaction(e.getCause.toString) => throw RequiredRelationWouldBeViolated(parentInfo, where)
    })
  }

  def causedByThisMutaction(cause: String) = {
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

    val parentCheckString = s"`${parentInfo.relation.id}` where `${parentInfo.relation.sideOf(where.model)}` ="
    val childCheckString  = s"`${parentInfo.relation.id}` where `${parentInfo.relation.sideOf(parentInfo.model)}` ="

    (cause.contains(parentCheckString) && cause.contains(parameterString(where))) ||
    (cause.contains(childCheckString) && cause.contains(parameterString(parentInfo.where)))
  }

}
