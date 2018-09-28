package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector.mongo.NestedDatabaseMutactionInterpreter
import com.prisma.api.connector.mongo.database.{MongoAction, MongoActionsBuilder}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.{Relation, RelationField}

import scala.concurrent.ExecutionContext

trait NestedRelationInterpreterBase extends NestedDatabaseMutactionInterpreter {
  def relationField: RelationField
  def relation: Relation = relationField.relation
  val p                  = relationField
  val c                  = relationField.relatedField

  implicit def ec: ExecutionContext

  val noCheckRequired                = MongoAction.successful(())
  val noActionRequired               = MongoAction.successful(())
  def requiredRelationViolation      = throw RequiredRelationWouldBeViolated(relation)
  def errorBecauseManySideIsRequired = sys.error("This should not happen, since it means a many side is required")

  def removalByParent(parentId: IdGCValue)(implicit mutationBuilder: MongoActionsBuilder) = {
//    mutationBuilder.deleteRelationRowByParentId(relationField, parentId)
    ???
  }

  def checkForOldChild(parentId: IdGCValue)(implicit mutationBuilder: MongoActionsBuilder) = {
//    mutationBuilder.ensureThatNodeIsNotConnected(relationField.relatedField, parentId)
    ???
  }
}
