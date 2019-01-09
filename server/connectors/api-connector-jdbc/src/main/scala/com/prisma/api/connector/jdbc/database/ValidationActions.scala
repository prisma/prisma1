package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.NodeSelector
import com.prisma.api.schema.APIErrors.{NodesNotConnectedError, RequiredRelationWouldBeViolated}
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.RelationField

import scala.concurrent.ExecutionContext

trait ValidationActions extends BuilderBase with FilterConditionBuilder {
  import slickDatabase.profile.api._

  def ensureThatNodeIsNotConnected(relationField: RelationField, id: IdGCValue): DBIO[Unit] = {
    val relation = relationField.relation
    val idQuery = sql
      .select(relationColumn(relation, relationField.oppositeRelationSide))
      .from(relationTable(relation))
      .where(
        relationColumn(relation, relationField.oppositeRelationSide).equal(placeHolder),
        relationColumn(relation, relationField.relationSide).isNotNull
      )

    queryToDBIO(idQuery)(
      setParams = _.setGcValue(id),
      readResult = rs => {
        if (rs.next)
          throw RequiredRelationWouldBeViolated(relation)
      }
    )
  }

  def ensureThatNodesAreConnected(relationField: RelationField, childId: IdGCValue, parentId: IdGCValue)(implicit ec: ExecutionContext): DBIO[Unit] = {
    val relation = relationField.relation
    val idQuery = sql
      .select(relationColumn(relation, relationField.oppositeRelationSide))
      .from(relationTable(relation))
      .where(
        relationColumn(relation, relationField.oppositeRelationSide).equal(placeHolder),
        relationColumn(relation, relationField.relationSide).equal(placeHolder)
      )

    queryToDBIO(idQuery)(
      setParams = { pp =>
        pp.setGcValue(childId)
        pp.setGcValue(parentId)
      },
      readResult = rs => {
        if (!rs.next)
          throw NodesNotConnectedError(
            relation = relationField.relation,
            parent = relationField.model,
            parentWhere = Some(NodeSelector.forId(relationField.model, parentId)),
            child = relationField.relatedModel_!,
            childWhere = Some(NodeSelector.forId(relationField.relatedModel_!, childId))
          )
      }
    )
  }

  def ensureThatParentIsConnected(
      relationField: RelationField,
      parentId: IdGCValue
  )(implicit ec: ExecutionContext): DBIO[Unit] = {
    val relation = relationField.relation
    val idQuery = sql
      .select(relationColumn(relation, relationField.relationSide))
      .from(relationTable(relation))
      .where(
        relationColumn(relation, relationField.relationSide).equal(placeHolder),
        relationColumn(relation, relationField.oppositeRelationSide).isNotNull
      )

    queryToDBIO(idQuery)(
      setParams = _.setGcValue(parentId),
      readResult = rs => {
        if (!rs.next)
          throw NodesNotConnectedError(
            relation = relationField.relation,
            parent = relationField.model,
            parentWhere = Some(NodeSelector.forId(relationField.model, parentId)),
            child = relationField.relatedModel_!,
            childWhere = None
          )
      }
    )
  }

  def errorIfNodeIsInRelation(parentId: IdGCValue, field: RelationField)(implicit ec: ExecutionContext): DBIO[Unit] = {
    errorIfNodesAreInRelation(Vector(parentId), field)
  }

  def errorIfNodesAreInRelation(parentIds: Vector[IdGCValue], field: RelationField)(implicit ec: ExecutionContext): DBIO[Unit] = {
    val relation = field.relation
    val query = sql
      .select(relationColumn(relation, field.oppositeRelationSide))
      .from(relationTable(relation))
      .where(
        relationColumn(relation, field.oppositeRelationSide).in(placeHolders(parentIds)),
        relationColumn(relation, field.relationSide).isNotNull
      )

    queryToDBIO(query)(
      setParams = pp => parentIds.foreach(pp.setGcValue),
      readResult = rs => if (rs.next) throw RequiredRelationWouldBeViolated(relation)
    )
  }
}
