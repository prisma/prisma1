package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.schema.APIErrors.{NodesNotConnectedError, RequiredRelationWouldBeViolated}
import com.prisma.gc_values.{IdGCValue, ListGCValue, NullGCValue}
import com.prisma.shared.models.RelationField

import scala.concurrent.{ExecutionContext, Future}

trait ValidationActions extends FilterConditionBuilder with NodeSingleQueries with NodeManyQueries {

  def ensureThatNodeIsNotConnected(relationField: RelationField, id: IdGCValue)(implicit ec: ExecutionContext) = {
    relationField.relationIsInlinedInParent match {
      case true =>
        getNodeByWhere(NodeSelector.forId(relationField.model, id)).map(optionRes =>
          optionRes.foreach { res =>
            (relationField.isList, res.data.map.get(relationField.name)) match {
              case (true, Some(ListGCValue(values))) if values.isEmpty => throw RequiredRelationWouldBeViolated(relationField.relation)
              case (false, Some(x: IdGCValue))                         => throw RequiredRelationWouldBeViolated(relationField.relation)
              case (_, _)                                              => Future.successful(())
            }
        })

      case false =>
        val filter = generateFilterForFieldAndId(relationField.relatedField, id)

        getNodeIdsByFilter(relationField.relatedModel_!, Some(filter)).map(list =>
          if (list.nonEmpty) throw RequiredRelationWouldBeViolated(relationField.relation))
    }
  }

  def ensureThatNodesAreConnected(relationField: RelationField, childId: IdGCValue, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    val parentModel = relationField.model
    val childModel  = relationField.relatedModel_!

    relationField.relationIsInlinedInParent match {
      case true =>
        getNodeByWhere(NodeSelector.forId(parentModel, parentId)).map(optionRes =>
          optionRes.foreach { res =>
            (relationField.isList, res.data.map.get(relationField.name)) match {
              case (true, Some(ListGCValue(values))) if values.contains(childId) => Future.successful(())
              case (false, Some(x)) if x == childId                              => Future.successful(())
              case (_, _)                                                        => throw NodesNotConnectedError(relationField.relation, parentModel, None, relationField.relatedModel_!, None)
            }
        })

      case false =>
        val filter      = generateFilterForFieldAndId(relationField.relatedField, parentId)
        val whereFilter = ScalarFilter(childModel.idField_!, Equals(childId))

        getNodeIdsByFilter(relationField.relatedModel_!, Some(AndFilter(Vector(filter, whereFilter)))).map(
          list =>
            if (list.isEmpty)
              throw NodesNotConnectedError(
                relation = relationField.relation,
                parent = relationField.model,
                parentWhere = Some(NodeSelector.forId(relationField.model, parentId)),
                child = relationField.relatedModel_!,
                childWhere = None
            ))
    }
  }

  def ensureThatParentIsConnected(relationField: RelationField, parentId: IdGCValue)(implicit ec: ExecutionContext) = {
    val parentModel = relationField.model
    val childModel  = relationField.relatedModel_!

    relationField.relationIsInlinedInParent match {
      case true =>
        getNodeByWhere(NodeSelector.forId(parentModel, parentId)).map(optionRes =>
          optionRes.foreach { res =>
            res.data.map.get(relationField.name) match {
              case None              => throw NodesNotConnectedError(relationField.relation, parentModel, None, relationField.relatedModel_!, None)
              case Some(NullGCValue) => throw NodesNotConnectedError(relationField.relation, parentModel, None, relationField.relatedModel_!, None)
              case _                 => Future.successful(())
            }
        })

      case false =>
        val filter = generateFilterForFieldAndId(relationField.relatedField, parentId)

        getNodeIdsByFilter(childModel, Some(filter)).map(
          list =>
            if (list.isEmpty)
              throw NodesNotConnectedError(
                relation = relationField.relation,
                parent = relationField.model,
                parentWhere = Some(NodeSelector.forId(relationField.model, parentId)),
                child = relationField.relatedModel_!,
                childWhere = None
            ))
    }
  }

  def errorIfNodesAreInRelation(parentIds: Vector[IdGCValue], otherField: RelationField)(implicit ec: ExecutionContext) = {
    val otherModel   = otherField.model
    val relatedModel = otherField.relatedModel_!

    otherField.relationIsInlinedInParent match {
      case true =>
        val filter = otherField.isList match {
          case true  => ScalarListFilter(otherModel.idField_!.copy(name = otherField.dbName, isList = true), ListContainsSome(parentIds))
          case false => ScalarFilter(otherModel.idField_!.copy(name = otherField.dbName), In(parentIds))
        }
        getNodeIdsByFilter(otherModel, Some(filter)).map(list => if (list.nonEmpty) throw RequiredRelationWouldBeViolated(otherField.relation))

      case false =>
        val filter = ScalarFilter(relatedModel.idField_!, In(parentIds))
        getNodesByFilter(relatedModel, Some(filter)).map { list =>
          list.foreach { doc =>
            if (!otherField.relatedField.isList && doc.get(otherField.relatedField.dbName).isDefined) throw RequiredRelationWouldBeViolated(otherField.relation)
          }
        }
    }
  }

  def errorIfNodeIsInRelation(nodeId: IdGCValue, otherField: RelationField)(implicit ec: ExecutionContext) = {
    val otherModel   = otherField.model
    val relatedModel = otherField.relatedModel_!

    otherField.relationIsInlinedInParent match {
      case true =>
        val filter = generateFilterForFieldAndId(otherField, nodeId)

        getNodeIdsByFilter(otherModel, Some(filter)).map(list => if (list.nonEmpty) throw RequiredRelationWouldBeViolated(otherField.relation))

      case false =>
        val filter = ScalarFilter(relatedModel.idField_!, Equals(nodeId))
        getNodesByFilter(relatedModel, Some(filter)).map { list =>
          list.foreach { doc =>
            if (!otherField.relatedField.isList && doc.get(otherField.relatedField.dbName).isDefined) throw RequiredRelationWouldBeViolated(otherField.relation)
          }
        }
    }
  }

  private def generateFilterForFieldAndId(relationField: RelationField, id: IdGCValue) = relationField.isList match {
    case true  => ScalarFilter(relationField.model.idField_!.copy(name = relationField.dbName), Contains(id))
    case false => ScalarFilter(relationField.model.idField_!.copy(name = relationField.dbName), Equals(id))
  }
}
