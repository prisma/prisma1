package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.mutations.{NodeSelector, ParentInfo}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.Project

case class NestedDisconnectRelationMutaction(project: Project, parentInfo: ParentInfo, where: NodeSelector, topIsCreate: Boolean = false)
    extends NestedRelationMutactionBaseClass {

  override def requiredCheck =
    (p.isList, p.isRequired, c.isList, c.isRequired) match {
      case (false, true, false, true)   => throw RequiredRelationWouldBeViolated(parentInfo, where)
      case (false, true, false, false)  => throw RequiredRelationWouldBeViolated(parentInfo, where)
      case (false, false, false, true)  => throw RequiredRelationWouldBeViolated(parentInfo, where)
      case (false, false, false, false) => List.empty
      case (true, false, false, true)   => throw RequiredRelationWouldBeViolated(parentInfo, where)
      case (true, false, false, false)  => List.empty
      case (false, true, true, false)   => throw RequiredRelationWouldBeViolated(parentInfo, where)
      case (false, false, true, false)  => List.empty
      case (true, false, true, false)   => List.empty
      case _                            => sys.error("This should not happen, since it means a many side is required")
    }

  override def removalActions = List(removalByParentAndChild)

  override def addAction = List.empty
}
