package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.NodesNotConnectedError
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.{Model, RelationField, Schema}
import slick.dbio._

import scala.concurrent.ExecutionContext

case class DeleteNodeInterpreter(mutaction: TopLevelDeleteNode, shouldDeleteRelayIds: Boolean)(implicit val ec: ExecutionContext)
    extends TopLevelDatabaseMutactionInterpreter
    with CascadingDeleteSharedStuff {

  override def schema = mutaction.where.model.schema

  override def dbioAction(mutationBuilder: JdbcActionsBuilder) = {
    for {
      nodeOpt <- mutationBuilder.getNodeByWhere(mutaction.where)
      node <- nodeOpt match {
               case Some(node) =>
                 for {
                   _ <- performCascadingDelete(mutationBuilder, mutaction.where.model, node.id)
                   _ <- checkForRequiredRelationsViolations(mutationBuilder, node.id)
                   _ <- mutationBuilder.deleteNodeById(mutaction.where.model, node.id, shouldDeleteRelayIds)
                 } yield node
               case None =>
                 DBIO.failed(APIErrors.NodeNotFoundForWhereError(mutaction.where))
             }
    } yield DeleteNodeResult(node.id, node, mutaction)
  }

  private def checkForRequiredRelationsViolations(mutationBuilder: JdbcActionsBuilder, id: IdGCValue): DBIO[_] = {
    val fieldsWhereThisModelIsRequired = schema.fieldsWhereThisModelIsRequired(mutaction.where.model)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.errorIfNodeIsInRelation(id, field))
    DBIO.sequence(actions)
  }
}

case class NestedDeleteNodeInterpreter(mutaction: NestedDeleteNode, shouldDeleteRelayIds: Boolean)(implicit val ec: ExecutionContext)
    extends NestedDatabaseMutactionInterpreter
    with CascadingDeleteSharedStuff {

  override def schema = mutaction.project.schema
  val parentField     = mutaction.relationField
  val parent          = mutaction.relationField.model
  val child           = mutaction.relationField.relatedModel_!

  override def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue) = {
    for {
      childId <- getChildId(mutationBuilder, parentId)
      _       <- mutationBuilder.ensureThatNodesAreConnected(parentField, childId, parentId)
      _       <- performCascadingDelete(mutationBuilder, child, childId)
      _       <- checkForRequiredRelationsViolations(mutationBuilder, childId)
      _       <- mutationBuilder.deleteNodeById(child, childId, shouldDeleteRelayIds)
    } yield UnitDatabaseMutactionResult
  }

  private def getChildId(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue): DBIO[IdGCValue] = {
    mutaction.where match {
      case Some(where) =>
        mutationBuilder.getNodeIdByWhere(where).map {
          case Some(id) => id
          case None     => throw APIErrors.NodeNotFoundForWhereError(where)
        }
      case None =>
        mutationBuilder.getNodeIdByParentId(parentField, parentId).map {
          case Some(id) => id
          case None =>
            throw NodesNotConnectedError(
              relation = parentField.relation,
              parent = parentField.model,
              parentWhere = Some(NodeSelector.forId(parent, parentId)),
              child = parentField.relatedModel_!,
              childWhere = None
            )
        }
    }
  }

  private def checkForRequiredRelationsViolations(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue): DBIO[_] = {
    val fieldsWhereThisModelIsRequired = mutaction.project.schema.fieldsWhereThisModelIsRequired(mutaction.relationField.relatedModel_!)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.errorIfNodeIsInRelation(parentId, field))
    DBIO.sequence(actions)
  }
}

trait CascadingDeleteSharedStuff {
  def shouldDeleteRelayIds: Boolean
  def schema: Schema
  implicit def ec: ExecutionContext

  def performCascadingDelete(mutationBuilder: JdbcActionsBuilder, model: Model, parentId: IdGCValue): DBIO[Unit] = {
    val actions = model.cascadingRelationFields.map(field => recurse(mutationBuilder = mutationBuilder, parentField = field, parentIds = Vector(parentId)))
    DBIO.seq(actions: _*)
  }

  private def recurse(
      mutationBuilder: JdbcActionsBuilder,
      parentField: RelationField,
      parentIds: Vector[IdGCValue],
      seenIds: Vector[IdGCValue] = Vector.empty
  ): DBIO[Unit] = {

    for {
      ids        <- mutationBuilder.getNodeIdsByParentIds(parentField, parentIds)
      groupedIds = ids.grouped(10000).toVector
      model      = parentField.relatedModel_!
      //nestedActions
      _ <- if (ids.isEmpty) {
            DBIO.successful(())
          } else {
            //children
            val cascadingChildren = model.cascadingRelationFields.filter(_ != parentField.relatedField)
            val childActions = for {
              field   <- cascadingChildren
              idGroup <- groupedIds
            } yield {
              recurse(mutationBuilder, field, idGroup)
            }
            //other parent
            val cascadingParent = model.cascadingRelationFields.filter(_ == parentField.relatedField)
            val parentActions = for {
              field           <- cascadingParent
              idGroup         <- groupedIds
              idGroupFiltered = idGroup.filter(x => !seenIds.contains(x))
            } yield {
              recurse(mutationBuilder, field, idGroupFiltered, parentIds)
            }

            DBIO.seq(childActions ++ parentActions: _*)
          }
      //actions for this level
      _ <- DBIO.seq(groupedIds.map(checkTheseOnes(mutationBuilder, parentField, _)): _*)
      _ <- DBIO.seq(groupedIds.map(mutationBuilder.deleteNodes(model, _, shouldDeleteRelayIds)): _*)
    } yield ()
  }

  private def checkTheseOnes(mutationBuilder: JdbcActionsBuilder, parentField: RelationField, parentIds: Vector[IdGCValue]) = {
    val model                          = parentField.relatedModel_!
    val fieldsWhereThisModelIsRequired = schema.fieldsWhereThisModelIsRequired(model).filter(_ != parentField)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.errorIfNodesAreInRelation(parentIds, field))
    DBIO.sequence(actions)
  }
}
