package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector.NodeAddress
import com.prisma.api.connector.mongo.NestedDatabaseMutactionInterpreter
import com.prisma.api.connector.mongo.database.{MongoAction, MongoActionsBuilder}
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
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

  def removalByParent(parent: NodeAddress)(implicit mutationBuilder: MongoActionsBuilder) = {
    mutationBuilder.deleteRelationRowByParent(relationField, parent)
  }

  def checkForOldChild(parent: NodeAddress)(implicit mutationBuilder: MongoActionsBuilder) = {
    assert(parent.path.segments.isEmpty)
    mutationBuilder.ensureThatNodeIsNotConnected(relationField, parent.where)
  }
}
