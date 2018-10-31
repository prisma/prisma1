package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{MongoActionsBuilder, SequenceAction, SuccessAction}
import com.prisma.api.connector.mongo.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.NodesNotConnectedError
import com.prisma.gc_values.{IdGCValue, StringIdGCValue}
import com.prisma.shared.models.{Model, Project}

import scala.concurrent.ExecutionContext

case class DeleteNodeInterpreter(mutaction: TopLevelDeleteNode)(implicit val ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {

  override def mongoAction(mutationBuilder: MongoActionsBuilder) = {
    for {
      nodeOpt <- mutationBuilder.getNodeByWhere(mutaction.where)
      node <- nodeOpt match {
               case Some(node) =>
                 for {
//            _ <- performCascadingDelete(mutationBuilder, mutaction.where.model, node.id)
                   _ <- DeleteShared.checkForRequiredRelationsViolations(mutaction.project, mutaction.model, mutationBuilder, node.id)
                   _ <- mutationBuilder.deleteNodeById(mutaction.where.model, node.id)
                 } yield node
               case None =>
                 throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
             }
    } yield MutactionResults(Vector(DeleteNodeResult(node, mutaction)))
  }
}

case class DeleteNodesInterpreter(mutaction: DeleteNodes)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {

  def mongoAction(mutationBuilder: MongoActionsBuilder) =
    for {
      ids <- mutationBuilder.getNodeIdsByFilter(mutaction.model, mutaction.whereFilter)
      _   <- checkForRequiredRelationsViolations(mutationBuilder, ids)
      _   <- mutationBuilder.deleteNodes(mutaction.model, ids)
    } yield MutactionResults(Vector(ManyNodesResult(mutaction, ids.size)))

  private def checkForRequiredRelationsViolations(mutationBuilder: MongoActionsBuilder, nodeIds: Seq[IdGCValue]) = {
    val fieldsWhereThisModelIsRequired = mutaction.project.schema.fieldsWhereThisModelIsRequired(mutaction.model)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.errorIfNodesAreInRelation(nodeIds.toVector, field))

    SequenceAction(actions.toVector)
  }
}

case class NestedDeleteNodeInterpreter(mutaction: NestedDeleteNode)(implicit val ec: ExecutionContext) extends NestedDatabaseMutactionInterpreter {
  val parentField = mutaction.relationField
  val child       = mutaction.relationField.relatedModel_!

  override def mongoAction(mutationBuilder: MongoActionsBuilder, parent: NodeAddress) = {
    for {
      childId <- getChildId(mutationBuilder, parent)
      _       <- mutationBuilder.ensureThatNodesAreConnected(parentField, childId, parent)
//      _       <- performCascadingDelete(mutationBuilder, child, childId)
      _ <- DeleteShared.checkForRequiredRelationsViolations(mutaction.project, mutaction.relationField.relatedModel_!, mutationBuilder, childId)
      _ <- mutationBuilder.deleteNodeById(child, childId)
      _ <- mutationBuilder.deleteRelationRowByChildIdAndParentId(parentField, childId, parent)
    } yield MutactionResults(Vector.empty)
  }

  private def getChildId(mutationBuilder: MongoActionsBuilder, parent: NodeAddress) = {
    mutaction.where match {
      case Some(where) =>
        mutationBuilder.getNodeIdByWhere(where).map {
          case Some(id) => id
          case None     => throw APIErrors.NodeNotFoundForWhereError(where)
        }
      case None =>
        mutationBuilder.getNodeIdByParent(parentField, parent).map {
          case Some(id) => id
          case None =>
            throw NodesNotConnectedError(
              relation = parentField.relation,
              parent = parentField.model,
              parentWhere = Some(parent.where),
              child = parentField.relatedModel_!,
              childWhere = None
            )
        }
    }
  }
}

case class NestedDeleteNodesInterpreter(mutaction: NestedDeleteNodes)(implicit val ec: ExecutionContext) extends NestedDatabaseMutactionInterpreter {

  val parentField = mutaction.relationField
  val child       = mutaction.relationField.relatedModel_!

  override def mongoAction(mutationBuilder: MongoActionsBuilder, parent: NodeAddress) = {
//    for {
//      childId <- getChildId(mutationBuilder, parent)
//      _       <- mutationBuilder.ensureThatNodesAreConnected(parentField, childId, parent)
//      //      _       <- performCascadingDelete(mutationBuilder, child, childId)
//      _ <- checkForRequiredRelationsViolations(mutationBuilder, childId)
//      _ <- mutationBuilder.deleteNodeById(child, childId)
//      _ <- mutationBuilder.deleteRelationRowByChildIdAndParentId(parentField, childId, parent)
//    } yield MutactionResults(Vector.empty)

    SuccessAction(MutactionResults(Vector.empty))
  }

  private def getChildId(mutationBuilder: MongoActionsBuilder, parent: NodeAddress) = {
//    mutaction.where match {
//      case Some(where) =>
//        mutationBuilder.getNodeIdByWhere(where).map {
//          case Some(id) => id
//          case None     => throw APIErrors.NodeNotFoundForWhereError(where)
//        }
//      case None =>
//        mutationBuilder.getNodeIdByParent(parentField, parent).map {
//          case Some(id) => id
//          case None =>
//            throw NodesNotConnectedError(
//              relation = parentField.relation,
//              parent = parentField.model,
//              parentWhere = Some(parent.where),
//              child = parentField.relatedModel_!,
//              childWhere = None
//            )
//        }
//    }
    StringIdGCValue.dummy
  }

  private def checkForRequiredRelationsViolations(mutationBuilder: MongoActionsBuilder, parentId: IdGCValue) = {
    val fieldsWhereThisModelIsRequired = mutaction.project.schema.fieldsWhereThisModelIsRequired(mutaction.relationField.relatedModel_!)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.errorIfNodeIsInRelation(parentId, field)).toVector
    SequenceAction(actions)
  }
}

object DeleteShared {

  def checkForRequiredRelationsViolations(project: Project, model: Model, mutationBuilder: MongoActionsBuilder, parentId: IdGCValue)(
      implicit ec: ExecutionContext) = {
    val fieldsWhereThisModelIsRequired = project.schema.fieldsWhereThisModelIsRequired(model)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.errorIfNodeIsInRelation(parentId, field)).toVector
    SequenceAction(actions)
  }
}
