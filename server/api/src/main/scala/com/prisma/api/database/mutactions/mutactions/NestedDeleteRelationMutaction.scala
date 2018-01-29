package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.mutations.{NodeSelector, ParentInfo}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.shared.models.{Field, Project, Relation}

case class NestedDeleteRelationMutaction(project: Project, parentInfo: ParentInfo, where: NodeSelector, topIsCreate: Boolean = false)
    extends NestedRelationMutactionBaseClass {

  val otherRelationFieldsOfChild          = where.model.relationFields.filter(field => !field.relation.contains(parentInfo.relation))
  val relationsWhereTheChildIsRequired    = otherRelationFieldsOfChild.filter(otherSideIsRequired).map(_.relation.get)
  val relationsWhereTheChildIsNotRequired = otherRelationFieldsOfChild.filter(otherSideIsNotRequired).map(_.relation.get)

  override def requiredCheck = {
    val checksForThisParentRelation = (p.isList, p.isRequired, c.isList, c.isRequired) match {
      case (false, true, false, true)   => requiredRelationViolation
      case (false, true, false, false)  => requiredRelationViolation
      case (false, false, false, true)  => noCheckRequired
      case (false, false, false, false) => noCheckRequired
      case (true, false, false, true)   => noCheckRequired
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

  override def handleErrors = {
    Some({
      case e: SQLException if e.getErrorCode == 1242 && causedByThisMutaction(parentInfo.relation, e.getCause.toString) =>
        throw RequiredRelationWouldBeViolated(project, parentInfo.relation)
      case e: SQLException if e.getErrorCode == 1242 && otherFailingRequiredRelationOnChild(e.getCause.toString).isDefined =>
        throw RequiredRelationWouldBeViolated(project, otherFailingRequiredRelationOnChild(e.getCause.toString).get)
    })
  }

  def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] = relationsWhereTheChildIsRequired.collectFirst {
    case x if causedByThisMutactionChildOnly(x, cause) => x
  }

  def otherSideIsRequired(field: Field): Boolean = field.relatedField(project.schema) match {
    case Some(f) if f.isRequired => true
    case Some(f)                 => false
    case None                    => false
  }

  def otherSideIsNotRequired(field: Field): Boolean = !otherSideIsRequired(field)

}
