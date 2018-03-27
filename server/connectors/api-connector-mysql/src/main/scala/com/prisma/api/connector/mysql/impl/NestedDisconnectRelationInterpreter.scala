package com.prisma.api.connector.mysql.impl

import com.prisma.api.connector.NestedDisconnectRelation

case class NestedDisconnectRelationInterpreter(mutaction: NestedDisconnectRelation) extends NestedRelationInterpreterBase {
  override def path        = mutaction.path
  override def project     = mutaction.project
  override def topIsCreate = mutaction.topIsCreate

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
