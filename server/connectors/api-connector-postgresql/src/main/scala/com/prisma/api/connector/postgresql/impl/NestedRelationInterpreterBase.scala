package com.prisma.api.connector.postgresql.impl

import com.prisma.api.connector.{NodeSelector, UnitDatabaseMutactionResult}
import com.prisma.api.connector.postgresql.DatabaseMutactionInterpreter
import com.prisma.api.connector.postgresql.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.{Relation, RelationField}
import slick.dbio.{DBIO, DBIOAction}

import scala.concurrent.ExecutionContext

trait NestedRelationInterpreterBase extends DatabaseMutactionInterpreter {
  def relationField: RelationField
  def relation: Relation = relationField.relation
  val p                  = relationField
  val c                  = relationField.relatedField

  implicit def ec: ExecutionContext

//  override def newAction(mutationBuilder: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
//    DBIOAction.seq(allActions(mutationBuilder, parentId): _*).andThen(DBIO.successful(UnitDatabaseMutactionResult))
//  }

  override def action(mb: PostgresApiDatabaseMutationBuilder) = ???

  def allActions(implicit mb: PostgresApiDatabaseMutationBuilder, parentId: IdGCValue) = {
    requiredCheck(parentId) ++ removalActions(parentId) ++ addAction(parentId)
  }

  def requiredCheck(parentId: IdGCValue)(implicit mb: PostgresApiDatabaseMutationBuilder): List[DBIO[_]]
  def removalActions(parentId: IdGCValue)(implicit mb: PostgresApiDatabaseMutationBuilder): List[DBIO[_]]
  def addAction(parentId: IdGCValue)(implicit mb: PostgresApiDatabaseMutationBuilder): List[DBIO[_]]

  def noCheckRequired           = List.empty
  def noActionRequired          = List.empty
  def requiredRelationViolation = throw RequiredRelationWouldBeViolated(relation)
  def sysError                  = sys.error("This should not happen, since it means a many side is required")

  def removalByParent(parentId: IdGCValue)(implicit mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
    mutationBuilder.deleteRelationRowByParentId(relationField, parentId)
  }

  def checkForOldChild(parentId: IdGCValue)(implicit mb: PostgresApiDatabaseMutationBuilder) = {
    mb.ensureThatNodeIsNotConnected(relationField.relatedField, parentId)
  }
}
