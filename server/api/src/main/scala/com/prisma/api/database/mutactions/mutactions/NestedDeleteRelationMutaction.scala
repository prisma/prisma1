package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.mutations.{NodeSelector, ParentInfo}
import com.prisma.shared.models.Project

case class NestedDeleteRelationMutaction(project: Project, parentInfo: ParentInfo, where: NodeSelector, topIsCreate: Boolean = false)
    extends NestedRelationMutactionBaseClass {

  val otherRelationFieldsOfChild = where.model.relationFields.filter(field => !field.relation.contains(parentInfo.relation))

  val relationsWhereTheChildIsRequired    = otherRelationFieldsOfChild.filter(field => field.relatedField(project.schema).get.isRequired).map(_.relation.get)
  val relationsWhereTheChildIsNotRequired = otherRelationFieldsOfChild.filter(field => !field.relatedField(project.schema).get.isRequired).map(_.relation.get)

  override def requiredCheck = {
    val checksForThisParentRelation = (p.isList, p.isRequired, c.isList, c.isRequired) match {
      case (false, true, false, true)   => requiredRelationViolation
      case (false, true, false, false)  => requiredRelationViolation
      case (false, false, false, true)  => noCheckRequired
      case (false, false, false, false) => noCheckRequired
      case (true, false, false, true)   => requiredRelationViolation
      case (true, false, false, false)  => noCheckRequired
      case (false, true, true, false)   => requiredRelationViolation
      case (false, false, true, false)  => noCheckRequired
      case (true, false, true, false)   => noCheckRequired
      case _                            => sysError
    }

    val checksForOtherRelationsOfTheChild = relationsWhereTheChildIsRequired.map { relation =>
      DatabaseMutationBuilder.oldParentFailureTriggerForRequiredRelations(project, relation, where)
    }

    checksForThisParentRelation ++ checksForOtherRelationsOfTheChild
  }

  override def removalActions = {

    val removeChildFromOtherRelationRows = relationsWhereTheChildIsNotRequired.map { relation =>
      DatabaseMutationBuilder.deleteRelationRowByChild(project.id, relation, where)
    }

    List(removalByChild) ++ removeChildFromOtherRelationRows
  }

  override def addAction = noActionRequired
}
