package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector.jdbc.DatabaseMutactionInterpreter
import com.prisma.api.connector.jdbc.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.{Relation, RelationField}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

trait NestedRelationInterpreterBase extends DatabaseMutactionInterpreter {
  def relationField: RelationField
  def relation: Relation = relationField.relation
  val p                  = relationField
  val c                  = relationField.relatedField

  implicit def ec: ExecutionContext

  val noCheckRequired                = DBIO.successful(())
  val noActionRequired               = DBIO.successful(())
  def requiredRelationViolation      = throw RequiredRelationWouldBeViolated(relation)
  def errorBecauseManySideIsRequired = sys.error("This should not happen, since it means a many side is required")

  def removalByParent(parentId: IdGCValue)(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
    mutationBuilder.deleteRelationRowByParentId(relationField, parentId)
  }

  def checkForOldChild(parentId: IdGCValue)(implicit mb: PostgresApiDatabaseMutationBuilder) = {
    mb.ensureThatNodeIsNotConnected(relationField.relatedField, parentId)
  }
}
