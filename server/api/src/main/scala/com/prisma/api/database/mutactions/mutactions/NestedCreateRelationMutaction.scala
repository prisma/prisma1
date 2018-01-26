package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.mutations.{NodeSelector, ParentInfo}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.Project
import slick.dbio.{Effect, NoStream}
import slick.sql.{SqlAction, SqlStreamingAction}

case class NestedCreateRelationMutaction(project: Project, parentInfo: ParentInfo, where: NodeSelector, topIsCreate: Boolean)
    extends NestedRelationMutactionBaseClass {

  override def requiredCheck: List[SqlStreamingAction[Vector[Int], Int, Effect]] = topIsCreate match {
    case false =>
      (p.isList, p.isRequired, c.isList, c.isRequired) match {
        case (false, true, false, true)   => throw RequiredRelationWouldBeViolated(parentInfo, where)
        case (false, true, false, false)  => List.empty
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
        case (false, true, false, true)   => List.empty
        case (false, true, false, false)  => List.empty
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

  override def removalActions: List[SqlStreamingAction[Vector[Int], Int, Effect]#ResultAction[Int, NoStream, Effect]] = topIsCreate match {
    case false =>
      (p.isList, p.isRequired, c.isList, c.isRequired) match {
        case (false, true, false, true)   => throw RequiredRelationWouldBeViolated(parentInfo, where)
        case (false, true, false, false)  => List(removalByParent)
        case (false, false, false, true)  => List(removalByParent)
        case (false, false, false, false) => List(removalByParent)
        case (true, false, false, true)   => List.empty
        case (true, false, false, false)  => List.empty
        case (false, true, true, false)   => List(removalByParent)
        case (false, false, true, false)  => List(removalByParent)
        case (true, false, true, false)   => List.empty
        case _                            => sys.error("This should not happen, since it means a many side is required")
      }
    case true =>
      (p.isList, p.isRequired, c.isList, c.isRequired) match {
        case (false, true, false, true)   => List.empty
        case (false, true, false, false)  => List.empty
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

  override def addAction: List[SqlAction[Int, NoStream, Effect]] = createRelationRow
}
