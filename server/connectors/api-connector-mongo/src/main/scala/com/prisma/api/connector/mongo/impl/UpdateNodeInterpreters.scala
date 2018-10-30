package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{MongoAction, MongoActionsBuilder}
import com.prisma.api.connector.mongo.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.IdGCValue
import org.mongodb.scala.MongoWriteException

import scala.concurrent.ExecutionContext

case class UpdateNodeInterpreter(mutaction: TopLevelUpdateNode)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder): MongoAction[MutactionResults] = {
    mutationBuilder.updateNode(mutaction)
  }

  override val errorMapper = {
    case e: MongoWriteException if e.getError.getCode == 11000 && MongoErrorMessageHelper.getFieldOption(mutaction.model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(mutaction.model.name, MongoErrorMessageHelper.getFieldOption(mutaction.model, e).get)
  }
}

case class NestedUpdateNodeInterpreter(mutaction: NestedUpdateNode)(implicit ec: ExecutionContext) extends NestedDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder, parent: NodeAddress) = {
    for {
      _ <- verifyChildWhere(mutationBuilder, mutaction.where)
      childId <- mutaction.where match {
                  case Some(where) => mutationBuilder.getNodeIdByParentAndWhere(mutaction.relationField, parent, where)
                  case None        => mutationBuilder.getNodeIdByParent(mutaction.relationField, parent)
                }
      results <- childId match {
                  case Some(id) =>
                    mutationBuilder.updateNodeByWhere(mutaction, NodeSelector.forId(mutaction.model, id))
                  case None =>
                    throw APIErrors.NodesNotConnectedError(
                      relation = mutaction.relationField.relation,
                      parent = parent.where.model,
                      parentWhere = None,
                      child = mutaction.relationField.relatedModel_!,
                      childWhere = mutaction.where
                    )
                }
    } yield results
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

  override val errorMapper = {
    case e: MongoWriteException if e.getError.getCode == 11000 && MongoErrorMessageHelper.getFieldOption(mutaction.model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(mutaction.model.name, MongoErrorMessageHelper.getFieldOption(mutaction.model, e).get)
  }
}

case class NestedUpdateNodesInterpreter(mutaction: NestedUpdateNodes)(implicit ec: ExecutionContext) extends NestedDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder, parent: NodeAddress) = {
    for {
      ids <- mutationBuilder.getNodeIdsByFilter(mutaction.relationField.relatedModel_!, mutaction.whereFilter)
      _   <- mutationBuilder.updateNodes(mutaction, ids)
    } yield MutactionResults(Vector(ManyNodesResult(mutaction, ids.length)))
  }

  override val errorMapper = {
    case e: MongoWriteException if e.getError.getCode == 11000 && MongoErrorMessageHelper.getFieldOption(mutaction.model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(mutaction.model.name, MongoErrorMessageHelper.getFieldOption(mutaction.model, e).get)
  }
}
