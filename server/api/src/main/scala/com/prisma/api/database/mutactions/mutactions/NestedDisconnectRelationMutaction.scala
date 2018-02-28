package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.mutations.mutations.CascadingDeletes.Path
import com.prisma.shared.models.Project

case class NestedDisconnectRelationMutaction(project: Project, path: Path, topIsCreate: Boolean = false) extends NestedRelationMutactionBaseClass {

  override def requiredCheck =
    (p.isList, p.isRequired, c.isList, c.isRequired) match {
      case (false, true, false, true)   => requiredRelationViolation
      case (false, true, false, false)  => requiredRelationViolation
      case (false, false, false, true)  => requiredRelationViolation
      case (false, false, false, false) => noCheckRequired
      case (true, false, false, true)   => requiredRelationViolation
      case (true, false, false, false)  => noCheckRequired
      case (false, true, true, false)   => requiredRelationViolation
      case (false, false, true, false)  => noCheckRequired
      case (true, false, true, false)   => noCheckRequired
      case _                            => sysError
    }

  override def removalActions = List(removalByParentAndChild)

  override def addAction = noActionRequired
}
