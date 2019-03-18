package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database._
import com.prisma.api.connector.mongo.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.NodesNotConnectedError
import com.prisma.gc_values.{IdGCValue, ListGCValue}
import com.prisma.shared.models.{Model, Project, RelationField}

import scala.concurrent.ExecutionContext

case class DeleteNodeInterpreter(mutaction: TopLevelDeleteNode)(implicit val ec: ExecutionContext)
    extends TopLevelDatabaseMutactionInterpreter
    with CascadingDeleteSharedStuff {

  override def mongoAction(mutationBuilder: MongoActionsBuilder) = {
    for {
      nodeOpt <- mutationBuilder.getNodeByWhereComplete(mutaction.where)
      node <- nodeOpt match {
               case Some(node) =>
                 for {
                   _ <- performCascadingDelete(mutationBuilder, mutaction.where.model, Vector(node.id))
                   _ <- DeleteShared.checkForRequiredRelationsViolations(mutaction.project, mutaction.model, mutationBuilder, Vector(node.id))
                   _ <- mutationBuilder.deleteNodeById(mutaction.where.model, node.id)
                 } yield node
               case None =>
                 throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
             }
    } yield MutactionResults(Vector(DeleteNodeResult(node, mutaction)))
  }
}

case class DeleteNodesInterpreter(mutaction: TopLevelDeleteNodes)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {

  def mongoAction(mutationBuilder: MongoActionsBuilder) =
    for {
      ids <- mutationBuilder.getNodeIdsByFilter(mutaction.model, mutaction.whereFilter)
      _   <- DeleteShared.checkForRequiredRelationsViolations(mutaction.project, mutaction.model, mutationBuilder, ids)
      _   <- mutationBuilder.deleteNodes(mutaction.model, ids)
    } yield MutactionResults(Vector(ManyNodesResult(mutaction, ids.size)))
}

case class NestedDeleteNodeInterpreter(mutaction: NestedDeleteNode)(implicit val ec: ExecutionContext) extends NestedDatabaseMutactionInterpreter {
  val parentField = mutaction.relationField
  val child       = mutaction.relationField.relatedModel_!

  override def mongoAction(mutationBuilder: MongoActionsBuilder, parent: NodeAddress) = {
    for {
      childId <- getChildId(mutationBuilder, parent)
      _       <- mutationBuilder.ensureThatNodesAreConnected(parentField, childId, parent)
//      _       <- performCascadingDelete(mutationBuilder, child, childId)
      _ <- DeleteShared.checkForRequiredRelationsViolations(mutaction.project, mutaction.relationField.relatedModel_!, mutationBuilder, Vector(childId))
      _ <- mutationBuilder.deleteNodeById(child, childId)
      _ <- mutationBuilder.deleteRelationRowByChildIdAndParentId(parentField, childId, parent, fromDelete = true)
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

case class NestedDeleteNodesInterpreter(mutaction: NestedDeleteNodes)(implicit val ec: ExecutionContext)
    extends NestedDatabaseMutactionInterpreter
    with NodeSingleQueries {

  val parentField = mutaction.relationField
  val child       = mutaction.relationField.relatedModel_!

  override def mongoAction(mutationBuilder: MongoActionsBuilder, parent: NodeAddress) = {

    val relationField = mutaction.relationField
    for {
      filterOption <- relationField.relationIsInlinedInParent match {
                       case true =>
                         for {
                           optionRes <- getNodeByWhere(parent.where, SelectedFields.byFieldAndNodeAddress(parentField, parent))
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
//            _       <- performCascadingDelete(mutationBuilder, child, childId)
      _ <- DeleteShared.checkForRequiredRelationsViolations(mutaction.project, mutaction.model, mutationBuilder, idList)
      _ <- mutationBuilder.deleteNodes(child, idList)
      _ <- SequenceAction(idList.map(id => mutationBuilder.deleteRelationRowByChildIdAndParentId(parentField, id, parent, fromDelete = true)).toVector)

    } yield MutactionResults(Vector(ManyNodesResult(mutaction, idList.length)))
  }
}

object DeleteShared {
  def checkForRequiredRelationsViolations(project: Project, model: Model, mutationBuilder: MongoActionsBuilder, nodeIds: Seq[IdGCValue])(
      implicit ec: ExecutionContext) = {
    val fieldsWhereThisModelIsRequired = project.schema.fieldsWhereThisModelIsRequired(model)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.errorIfNodesAreInRelation(nodeIds.toVector, field))

    SequenceAction(actions.toVector)
  }
}

trait CascadingDeleteSharedStuff {
  implicit def ec: ExecutionContext

  def performCascadingDelete(mutationBuilder: MongoActionsBuilder, model: Model, startingIds: Vector[IdGCValue]): MongoAction[_] = {
    val actions = model.cascadingRelationFields.map(field => recurse(mutationBuilder = mutationBuilder, parentField = field, parentIds = startingIds))
    MongoAction.seq(actions.toVector)
  }

  private def recurse(
      mutationBuilder: MongoActionsBuilder,
      parentField: RelationField,
      parentIds: Vector[IdGCValue],
      idsThatCanBeIgnored: Vector[IdGCValue] = Vector.empty
  ): MongoAction[Unit] = {

    for {
      childIds        <- mutationBuilder.getNodeIdsByParentIds(parentField, parentIds)
      filteredIds     = childIds.filter(x => !idsThatCanBeIgnored.contains(x))
      childIdsGrouped = filteredIds.grouped(10000).toVector
      model           = parentField.relatedModel_!
      //nestedActions
      _ <- if (filteredIds.isEmpty) {
            MongoAction.successful(())
          } else {
            //children
            val cascadingChildrenFields = model.cascadingRelationFields.filter(_ != parentField.relatedField)
            val childActions = for {
              field        <- cascadingChildrenFields
              childIdGroup <- childIdsGrouped
            } yield {
              recurse(mutationBuilder, field, childIdGroup.toVector)
            }
            //other parent
            val cascadingBackRelationFieldOfParentField = model.cascadingRelationFields.find(_ == parentField.relatedField)
            val parentActions = for {
              field        <- cascadingBackRelationFieldOfParentField.toVector
              childIdGroup <- childIdsGrouped.map(_.toVector)
            } yield {
              recurse(mutationBuilder, field, childIdGroup, idsThatCanBeIgnored = parentIds)
            }

            MongoAction.seq((childActions ++ parentActions).toVector)
          }
      //actions for this level
      _ <- MongoAction.seq(childIdsGrouped.map(checkTheseOnes(mutationBuilder, parentField, _)))
      _ <- MongoAction.seq(childIdsGrouped.map(mutationBuilder.deleteNodes(model, _)))
    } yield ()
  }

  private def checkTheseOnes(mutationBuilder: MongoActionsBuilder, parentField: RelationField, parentIds: Seq[IdGCValue]) = {
    val model                          = parentField.relatedModel_!
    val fieldsWhereThisModelIsRequired = model.schema.fieldsWhereThisModelIsRequired(model).filter(_ != parentField)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.errorIfNodesAreInRelation(parentIds.toVector, field))
    MongoAction.seq(actions.toVector)
  }
}
