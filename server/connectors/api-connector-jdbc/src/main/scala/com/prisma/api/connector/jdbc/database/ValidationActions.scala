package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.{Filter, NodeSelector}
import com.prisma.api.schema.APIErrors.{NodesNotConnectedError, RequiredRelationWouldBeViolated}
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.{Model, RelationField}
import org.jooq.impl.DSL.{asterisk, name, field}

import scala.concurrent.ExecutionContext

trait ValidationActions extends BuilderBase with FilterConditionBuilder {
  import slickDatabase.profile.api._

  def ensureThatNodeIsNotConnected(
      relationField: RelationField,
      childId: IdGCValue
  )(implicit ec: ExecutionContext): DBIO[Unit] = {
    val relation = relationField.relation
    val idQuery = sql
      .select(asterisk())
      .from(relationTable(relation))
      .where(
        relationColumn(relation, relationField.oppositeRelationSide).equal(placeHolder),
        relationColumn(relation, relationField.relationSide).isNotNull
      )

    val action = queryToDBIO(idQuery)(
      setParams = _.setGcValue(childId),
      readResult = rs => rs.readWith(readsAsUnit)
    )
    action.map { result =>
      if (result.nonEmpty) throw RequiredRelationWouldBeViolated(relation)
    }
  }

  def ensureThatNodeIsConnected(
      relationField: RelationField,
      childId: IdGCValue
  )(implicit ec: ExecutionContext): DBIO[Unit] = {
    val relation = relationField.relation
    val idQuery = sql
      .select(asterisk())
      .from(relationTable(relation))
      .where(
        relationColumn(relation, relationField.oppositeRelationSide).equal(placeHolder),
        relationColumn(relation, relationField.relationSide).isNotNull
      )

    val action = queryToDBIO(idQuery)(
      setParams = _.setGcValue(childId),
      readResult = rs => rs.readWith(readsAsUnit)
    )
    action.map { result =>
      if (result.isEmpty)
        throw NodesNotConnectedError(
          relation = relationField.relation,
          parent = relationField.model,
          parentWhere = None,
          child = relationField.relatedModel_!,
          childWhere = Some(NodeSelector.forIdGCValue(relationField.relatedModel_!, childId))
        )
    }
  }

  def ensureThatNodesAreConnected(relationField: RelationField, childId: IdGCValue, parentId: IdGCValue)(implicit ec: ExecutionContext): DBIO[Unit] = {
    val relation = relationField.relation
    val idQuery = sql
      .select(asterisk())
      .from(relationTable(relation))
      .where(
        relationColumn(relation, relationField.oppositeRelationSide).equal(placeHolder),
        relationColumn(relation, relationField.relationSide).equal(placeHolder)
      )

    val action = queryToDBIO(idQuery)(
      setParams = { pp =>
        pp.setGcValue(childId)
        pp.setGcValue(parentId)
      },
      readResult = rs => rs.readWith(readsAsUnit)
    )
    action.map { result =>
      if (result.isEmpty)
        throw NodesNotConnectedError(
          relation = relationField.relation,
          parent = relationField.model,
          parentWhere = Some(NodeSelector.forIdGCValue(relationField.model, parentId)),
          child = relationField.relatedModel_!,
          childWhere = Some(NodeSelector.forIdGCValue(relationField.relatedModel_!, childId))
        )
    }
  }

  def ensureThatParentIsConnected(
      relationField: RelationField,
      parentId: IdGCValue
  )(implicit ec: ExecutionContext): DBIO[Unit] = {
    val relation = relationField.relation
    val idQuery = sql
      .select(asterisk())
      .from(relationTable(relation))
      .where(
        relationColumn(relation, relationField.relationSide).equal(placeHolder),
        relationColumn(relation, relationField.oppositeRelationSide).isNotNull
      )

    val action = queryToDBIO(idQuery)(
      setParams = _.setGcValue(parentId),
      readResult = rs => rs.readWith(readsAsUnit)
    )
    action.map { result =>
      if (result.isEmpty)
        throw NodesNotConnectedError(
          relation = relationField.relation,
          parent = relationField.model,
          parentWhere = Some(NodeSelector.forIdGCValue(relationField.model, parentId)),
          child = relationField.relatedModel_!,
          childWhere = None
        )
    }
  }

  def errorIfNodeIsInRelation(parentId: IdGCValue, field: RelationField)(implicit ec: ExecutionContext): DBIO[Unit] = {
    errorIfNodesAreInRelation(Vector(parentId), field)
  }

  def errorIfNodesAreInRelation(parentIds: Vector[IdGCValue], field: RelationField)(implicit ec: ExecutionContext): DBIO[Unit] = {
    val relation = field.relation
    val query = sql
      .select(asterisk())
      .from(relationTable(relation))
      .where(
        relationColumn(relation, field.oppositeRelationSide).in(placeHolders(parentIds)),
        relationColumn(relation, field.relationSide).isNotNull
      )

    val action = queryToDBIO(query)(
      setParams = pp => parentIds.foreach(pp.setGcValue),
      readResult = rs => rs.readWith(readsAsUnit)
    )
    action.map { result =>
      if (result.nonEmpty) {
        // fixme: decide which error to use
        throw RequiredRelationWouldBeViolated(relation)
        //        throw RelationIsRequired(field.name, field.model.name)
      }
    }
  }
}
