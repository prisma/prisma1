package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.schema.APIErrors.{NodesNotConnectedError, RequiredRelationWouldBeViolated}
import com.prisma.gc_values.{IdGCValue, ListGCValue}
import com.prisma.shared.models.RelationField

import scala.concurrent.{ExecutionContext, Future}

trait ValidationActions extends FilterConditionBuilder with NodeSingleQueries with NodeManyQueries {

  def ensureThatNodeIsNotConnected(relationField: RelationField, where: NodeSelector)(implicit ec: ExecutionContext) = {
    for {
      filterOption <- relationField.relationIsInlinedInParent match {
                       case true =>
                         for {
                           optionRes <- getNodeByWhere(where, SelectedFields.forRelationField(relationField))
                           filterOption = optionRes.flatMap { res =>
                             (relationField.isList, res.data.map.get(relationField.name)) match {
                               case (true, Some(ListGCValue(values))) => Some(ScalarFilter(relationField.relatedModel_!.idField_!, In(values)))
                               case (false, Some(x: IdGCValue))       => Some(ScalarFilter(relationField.relatedModel_!.idField_!, Equals(x)))
                               case (_, _)                            => None
                             }
                           }
                         } yield filterOption
                       case false =>
                         MongoAction.successful(Some(generateFilterForFieldAndId(relationField.relatedField, where.fieldGCValue.asInstanceOf[IdGCValue])))
                     }
      list <- filterOption match {
               case Some(f) => getNodeIdsByFilter(relationField.relatedModel_!, Some(f))
               case None    => MongoAction.successful(List.empty)
             }
    } yield if (list.nonEmpty) throw RequiredRelationWouldBeViolated(relationField.relation)
  }

  def ensureThatParentIsConnected(relationField: RelationField, parent: NodeAddress)(implicit ec: ExecutionContext) = {
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
      list <- filterOption match {
               case Some(f) => getNodeIdsByFilter(relationField.relatedModel_!, Some(f))
               case None    => MongoAction.successful(List.empty)
             }
    } yield
      if (list.isEmpty)
        throw NodesNotConnectedError(
          relation = relationField.relation,
          parent = relationField.model,
          parentWhere = Some(parent.where),
          child = relationField.relatedModel_!,
          childWhere = None
        )
  }

  def errorIfNodeIsInRelation(nodeId: IdGCValue, otherField: RelationField)(implicit ec: ExecutionContext) =
    errorIfNodesAreInRelation(Vector(nodeId), otherField)

  def errorIfNodesAreInRelation(parentIds: Vector[IdGCValue], otherField: RelationField)(implicit ec: ExecutionContext) = {
    val otherModel   = otherField.model
    val relatedModel = otherField.relatedModel_!
    val relatedField = otherField.relatedField

    for {
      filter <- otherField.relationIsInlinedInParent match {
                 case false =>
                   val filter = ScalarFilter(relatedModel.idField_!, In(parentIds))
                   for {
                     result <- getNodes(relatedModel, QueryArguments.withFilter(filter), SelectedFields.forRelationField(relatedField))
                     ids = result.nodes.flatMap { node =>
                       (otherField.relatedField.isList, node.data.map.get(relatedField.name)) match {
                         case (true, Some(ListGCValue(values))) => values
                         case (false, Some(x: IdGCValue))       => Vector(x)
                         case (_, _)                            => Vector.empty
                       }
                     }
                   } yield ScalarFilter(otherModel.idField_!, In(ids))

                 case true =>
                   val filter = otherField.isList match {
                     case true  => ScalarListFilter(otherModel.dummyField(otherField), ListContainsSome(parentIds))
                     case false => ScalarFilter(otherModel.dummyField(otherField), In(parentIds))
                   }
                   MongoAction.successful(filter)
               }
      list <- getNodeIdsByFilter(otherModel, Some(filter))

    } yield
      if (list.nonEmpty)
        throw RequiredRelationWouldBeViolated(otherField.relation)
  }

  //here the ids do not need to be validated since we fetched them ourselves
  def ensureThatNodesAreConnected(relationField: RelationField, childId: IdGCValue, parent: NodeAddress)(implicit ec: ExecutionContext) = {
    val parentModel = relationField.model
    val childModel  = relationField.relatedModel_!

    relationField.relationIsInlinedInParent match {
      case true =>
        getNodeByWhere(parent.where, SelectedFields.byFieldAndNodeAddress(relationField, parent)).map(optionRes =>
          optionRes.foreach { res =>
            val optionNode = PrismaNode.getNodeAtPath(Some(res), parent.path.segments)

            optionNode match {
              case None => throw NodesNotConnectedError(relationField.relation, parentModel, None, relationField.relatedModel_!, None)
              case Some(node) =>
                (relationField.isList, node.data.map.get(relationField.name)) match {
                  case (true, Some(ListGCValue(values))) if values.contains(childId) => Future.successful(())
                  case (false, Some(x)) if x == childId                              => Future.successful(())
                  case (_, _)                                                        => throw NodesNotConnectedError(relationField.relation, parentModel, None, relationField.relatedModel_!, None)
                }
            }
        })

      case false =>
        val filter      = generateFilterForFieldAndId(relationField.relatedField, parent.idValue)
        val whereFilter = ScalarFilter(childModel.idField_!, Equals(childId))

        getNodeIdsByFilter(relationField.relatedModel_!, Some(AndFilter(Vector(filter, whereFilter)))).map(
          list =>
            if (list.isEmpty)
              throw NodesNotConnectedError(
                relation = relationField.relation,
                parent = relationField.model,
                parentWhere = Some(parent.where),
                child = relationField.relatedModel_!,
                childWhere = None
            ))
    }
  }
}
