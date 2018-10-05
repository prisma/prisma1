package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{MongoAction, MongoActionsBuilder, SimpleMongoAction}
import com.prisma.api.connector.mongo.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.{IdGCValue, RootGCValue}

import scala.concurrent.ExecutionContext

case class UpdateNodeInterpreter(mutaction: TopLevelUpdateNode)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder): MongoAction[MutactionResults] = {
    mutationBuilder.updateNode(mutaction)
  }
}

case class NestedUpdateNodeInterpreter(mutaction: NestedUpdateNode)(implicit ec: ExecutionContext) extends NestedDatabaseMutactionInterpreter {
  val model       = mutaction.relationField.relatedModel_!
  val parent      = mutaction.relationField.model
  val nonListArgs = mutaction.nonListArgs
  val listArgs    = mutaction.listArgs

  override def mongoAction(mutationBuilder: MongoActionsBuilder, parentId: IdGCValue) = {
    for {
      _ <- verifyChildWhere(mutationBuilder, mutaction.where)
      childId <- mutaction.where match {
                  case Some(where) => mutationBuilder.getNodeIdByParentIdAndWhere(mutaction.relationField, parentId, where)
                  case None        => mutationBuilder.getNodeIdByParentId(mutaction.relationField, parentId)
                }
      id <- childId match {
             case Some(id) => mutationBuilder.updateNodeByWhere(mutaction, NodeSelector.forId(mutaction.model, id)).map(_ => id)
             case None =>
               throw APIErrors.NodesNotConnectedError(
                 relation = mutaction.relationField.relation,
                 parent = parent,
                 parentWhere = None,
                 child = model,
                 childWhere = mutaction.where
               )
           }
    } yield MutactionResults(Vector(UpdateNodeResult(id, PrismaNode(id, RootGCValue.empty), mutaction)))
  }

  def verifyChildWhere(mutationBuilder: MongoActionsBuilder, where: Option[NodeSelector])(implicit ec: ExecutionContext) = {
    where match {
      case Some(where) =>
        for {
          id <- mutationBuilder.getNodeIdByWhere(where)
        } yield {
          if (id.isEmpty) throw APIErrors.NodeNotFoundForWhereError(where)
        }
      case None =>
        MongoAction.successful(())
    }
  }

}
