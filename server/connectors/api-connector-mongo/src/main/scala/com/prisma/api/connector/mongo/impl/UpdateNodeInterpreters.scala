package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{MongoAction, MongoActionsBuilder, NodeSingleQueries}
import com.prisma.api.connector.mongo.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.schema.{APIErrors, UserFacingError}
import com.prisma.gc_values.{IdGCValue, ListGCValue}
import com.prisma.shared.models.Model
import org.mongodb.scala.MongoWriteException

import scala.concurrent.ExecutionContext

case class UpdateNodeInterpreter(mutaction: TopLevelUpdateNode)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder): MongoAction[MutactionResults] = {
    mutationBuilder.updateNode(mutaction)
  }

  override val errorMapper = UpdateShared.errorHandler(mutaction.model)
}

case class UpdateNodesInterpreter(mutaction: UpdateNodes)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {
  def mongoAction(mutationBuilder: MongoActionsBuilder) =
    for {
      ids <- mutationBuilder.getNodeIdsByFilter(mutaction.model, mutaction.whereFilter)
      _   <- mutationBuilder.updateNodes(mutaction, ids)
    } yield MutactionResults(Vector(ManyNodesResult(mutaction, ids.size)))

  override val errorMapper = UpdateShared.errorHandler(mutaction.model)
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

  override val errorMapper = UpdateShared.errorHandler(mutaction.model)
}

case class NestedUpdateNodesInterpreter(mutaction: NestedUpdateNodes)(implicit ec: ExecutionContext)
    extends NestedDatabaseMutactionInterpreter
    with NodeSingleQueries {
  override def mongoAction(mutationBuilder: MongoActionsBuilder, parent: NodeAddress) = {

    val relationField = mutaction.relationField
    for {
      filterOption <- relationField.relationIsInlinedInParent match {
                       case true =>
                         for {
                           optionRes <- getNodeByWhere(parent.where, SelectedFields.byFieldAndNodeAddress(relationField, parent))
                           filterOption = PrismaNode.getNodeAtPath(optionRes, parent.path.segments).flatMap { res =>
                             (relationField.isList, res.data.map.get(relationField.name)) match {
                               case (true, Some(ListGCValue(values))) => Some(ScalarFilter(relationField.relatedModel_!.idField_!, In(values)))
                               case (false, Some(x: IdGCValue))       => Some(ScalarFilter(relationField.relatedModel_!.idField_!, Equals(x)))
                               case (_, _)                            => None
                             }
                           }
                         } yield filterOption
                       case false =>
                         MongoAction.successful(Some(generateFilterForFieldAndId(relationField.relatedField, parent.idValue)))
                     }
      idList <- filterOption match {
                 case Some(f) => getNodeIdsByFilter(relationField.relatedModel_!, Some(AndFilter(Vector(f) ++ mutaction.whereFilter)))
                 case None    => MongoAction.successful(List.empty)
               }
      _ <- mutationBuilder.updateNodes(mutaction, idList)

    } yield MutactionResults(Vector(ManyNodesResult(mutaction, idList.length)))
  }

  override val errorMapper = UpdateShared.errorHandler(mutaction.model)
}

object UpdateShared {
  def errorHandler(model: Model): PartialFunction[Throwable, UserFacingError] = {
    case e: MongoWriteException if e.getError.getCode == 11000 && MongoErrorMessageHelper.getFieldOption(model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(model.name, MongoErrorMessageHelper.getFieldOption(model, e).get)
    case e: MongoWriteException if e.getError.getCode == 40 =>
      APIErrors.MongoConflictingUpdates(model.name, e.getError.getMessage)

  }
}
