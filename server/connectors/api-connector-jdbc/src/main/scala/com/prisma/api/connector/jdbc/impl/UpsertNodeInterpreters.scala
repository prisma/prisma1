package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.api.connector.jdbc.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.connector.{NestedUpsertNode, TopLevelUpsertNode, UpsertNodeResult}
import com.prisma.gc_values.IdGCValue

import scala.concurrent.ExecutionContext

case class UpsertNodeInterpreter(mutaction: TopLevelUpsertNode)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {
  val model   = mutaction.where.model
  val project = mutaction.project

  override def dbioAction(mutationBuilder: JdbcActionsBuilder) = {
    for {
      id <- mutationBuilder.getNodeIdByWhere(mutaction.where)
    } yield
      id match {
        case Some(_) => UpsertNodeResult(mutaction.update, mutaction)
        case None    => UpsertNodeResult(mutaction.create, mutaction)
      }
  }
}

case class NestedUpsertNodeInterpreter(mutaction: NestedUpsertNode)(implicit ec: ExecutionContext) extends NestedDatabaseMutactionInterpreter {
  val model = mutaction.relationField.relatedModel_!

  override def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue) = {
    for {
      id <- mutaction.where match {
             case Some(where) => mutationBuilder.getNodeIdByParentIdAndWhere(mutaction.relationField, parentId, where)
             case None        => mutationBuilder.getNodeIdByParentId(mutaction.relationField, parentId)
           }
    } yield
      id match {
        case Some(_) => UpsertNodeResult(mutaction.update, mutaction)
        case None    => UpsertNodeResult(mutaction.create, mutaction)
      }
  }
}
