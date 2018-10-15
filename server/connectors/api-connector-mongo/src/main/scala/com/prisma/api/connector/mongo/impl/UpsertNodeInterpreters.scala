package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.MongoActionsBuilder
import com.prisma.api.connector.mongo.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.gc_values.IdGCValue

import scala.concurrent.ExecutionContext

case class UpsertNodeInterpreter(mutaction: TopLevelUpsertNode)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {
  val model   = mutaction.where.model
  val project = mutaction.project

  override def mongoAction(mutationBuilder: MongoActionsBuilder) = {
    for {
      id <- mutationBuilder.getNodeIdByWhere(mutaction.where)
    } yield
      id match {
        case Some(_) => MutactionResults(Vector(UpsertNodeResult(mutaction.update, mutaction)))
        case None    => MutactionResults(Vector(UpsertNodeResult(mutaction.create, mutaction)))
      }
  }
}

case class NestedUpsertNodeInterpreter(mutaction: NestedUpsertNode)(implicit ec: ExecutionContext) extends NestedDatabaseMutactionInterpreter {
  val model = mutaction.relationField.relatedModel_!

  override def mongoAction(mutationBuilder: MongoActionsBuilder, parent: NodeAddress) = {
    for {
      id <- mutaction.where match {
             case Some(where) => mutationBuilder.getNodeIdByParentAndWhere(mutaction.relationField, parent, where)
             case None        => mutationBuilder.getNodeIdByParent(mutaction.relationField, parent)
           }
    } yield
      id match {
        case Some(_) => MutactionResults(Vector(UpsertNodeResult(mutaction.update, mutaction)))
        case None    => MutactionResults(Vector(UpsertNodeResult(mutaction.create, mutaction)))
      }
  }
}
