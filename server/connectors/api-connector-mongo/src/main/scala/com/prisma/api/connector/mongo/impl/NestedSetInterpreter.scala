package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{MongoActionsBuilder, SequenceAction}
import com.prisma.api.schema.APIErrors

import scala.concurrent.ExecutionContext

case class NestedSetInterpreter(mutaction: NestedSet)(implicit val ec: ExecutionContext) extends NestedRelationInterpreterBase {
  override def relationField = mutaction.relationField

  override def mongoAction(mutationBuilder: MongoActionsBuilder, parent: NodeAddress) = {
    implicit val implicitMb = mutationBuilder
    SequenceAction(Vector(requiredCheck(parent), removalAction(parent), addAction(parent))).map(_ => MutactionResults(Vector.empty))
  }

  def requiredCheck(parent: NodeAddress)(implicit mutationBuilder: MongoActionsBuilder) = noCheckRequired

  def removalAction(parent: NodeAddress)(implicit mutationBuilder: MongoActionsBuilder) = c.isList match {
    case false => SequenceAction(Vector(removalByParent(parent), removalByChild))
    case true  => removalByParent(parent)
  }

  def removalByChild(implicit mutationBuilder: MongoActionsBuilder) = {
    SequenceAction(mutaction.wheres.map { where =>
      for {
        id <- mutationBuilder.getNodeIdByWhere(where)
        _ <- id match {
              case None          => throw APIErrors.NodeNotFoundForWhereError(where)
              case Some(childId) => mutationBuilder.deleteRelationRowByChildId(mutaction.relationField, childId)
            }
      } yield ()
    })
  }

  def checkForOldParentByChildWhere(implicit mutationBuilder: MongoActionsBuilder) = {
    SequenceAction(mutaction.wheres.map { where =>
      for {
        id <- mutationBuilder.getNodeIdByWhere(where)
        _ <- id match {
              case None          => throw APIErrors.NodeNotFoundForWhereError(where)
              case Some(childId) => mutationBuilder.ensureThatNodeIsNotConnected(relationField.relatedField, NodeSelector.forId(where.model, childId))
            }
      } yield ()
    })
  }

  def addAction(parent: NodeAddress)(implicit mutationBuilder: MongoActionsBuilder) = {
    SequenceAction(mutaction.wheres.map { where =>
      for {
        id <- mutationBuilder.getNodeIdByWhere(where)
        _ <- id match {
              case None          => throw APIErrors.NodeNotFoundForWhereError(where)
              case Some(childId) => mutationBuilder.createRelation(mutaction.relationField, parent, childId)
            }
      } yield ()
    })
  }

}
