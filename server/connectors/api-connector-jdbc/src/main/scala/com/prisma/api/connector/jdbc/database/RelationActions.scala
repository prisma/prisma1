package com.prisma.api.connector.jdbc.database

import com.prisma.gc_values.{StringIdGCValue, IdGCValue, NullGCValue}
import com.prisma.shared.models.{RelationField, RelationSide}
import cool.graph.cuid.Cuid

trait RelationActions extends BuilderBase {
  import slickDatabase.profile.api._

  def createRelation(relationField: RelationField, parentId: IdGCValue, childId: IdGCValue): DBIO[_] = {
    val relation = relationField.relation

    if (relation.isInlineRelation) {
      val inlineManifestation  = relation.inlineManifestation.get
      val referencingColumn    = inlineManifestation.referencingColumn
      val childWhereCondition  = idField(relationField.relatedModel_!).equal(placeHolder)
      val parentWhereCondition = idField(relationField.model).equal(placeHolder)

      val (rowToUpdateCondition, idToUpdate, idToLinkTo) = relationField.relationIsInlinedInParent match {
        case true  => (parentWhereCondition, parentId, childId)
        case false => (childWhereCondition, childId, parentId)
      }

      val query = sql
        .update(relationTable(relation))
        .setColumnsWithPlaceHolders(Vector(referencingColumn))
        .where(rowToUpdateCondition)

      updateToDBIO(query)(
        setParams = { pp =>
          pp.setGcValue(idToLinkTo)
          pp.setGcValue(idToUpdate)
        }
      )

    } else if (relation.relationTableHas3Columns) {
      val query = sql
        .insertInto(relationTable(relation))
        .columns(
          relationIdColumn(relation),
          relationColumn(relation, relationField.relationSide),
          relationColumn(relation, relationField.oppositeRelationSide)
        )
        .values(placeHolder, placeHolder, placeHolder)
        .onConflictDoNothing()

      insertToDBIO(query)(
        setParams = { pp =>
          pp.setGcValue(StringIdGCValue.random)
          pp.setGcValue(parentId)
          pp.setGcValue(childId)
        }
      )
    } else {
      val query = sql
        .insertInto(relationTable(relation))
        .columns(
          relationColumn(relation, relationField.relationSide),
          relationColumn(relation, relationField.oppositeRelationSide)
        )
        .values(placeHolder, placeHolder)
        .onConflictDoNothing()

      insertToDBIO(query)(
        setParams = { pp =>
          pp.setGcValue(parentId)
          pp.setGcValue(childId)
        }
      )
    }
  }

  def deleteRelationRowByChildId(relationField: RelationField, childId: IdGCValue): DBIO[_] = {
    assert(!relationField.relatedField.isList)
    val relation  = relationField.relation
    val condition = relationColumn(relation, relationField.oppositeRelationSide).equal(placeHolder)

    relation.inlineManifestation match {
      case Some(manifestation) =>
        val query = sql
          .update(relationTable(relation))
          .set(inlineRelationColumn(relation, manifestation), placeHolder)
          .where(condition)

        updateToDBIO(query)(
          setParams = { pp =>
            pp.setGcValue(NullGCValue)
            pp.setGcValue(childId)
          }
        )

      case None =>
        val query = sql
          .deleteFrom(relationTable(relation))
          .where(condition)

        deleteToDBIO(query)(setParams = _.setGcValue(childId))
    }
  }

  def deleteRelationRowByChildIdAndParentId(relationField: RelationField, childId: IdGCValue, parentId: IdGCValue): DBIO[_] = {
    val relation = relationField.relation
    val condition = relationColumn(relation, relationField.oppositeRelationSide)
      .equal(placeHolder)
      .and(relationColumn(relation, relationField.relationSide).equal(placeHolder))

    relation.inlineManifestation match {
      case Some(manifestation) =>
        val query = sql
          .update(relationTable(relation))
          .set(inlineRelationColumn(relation, manifestation), placeHolder)
          .where(condition)

        updateToDBIO(query)(
          setParams = { pp =>
            pp.setGcValue(NullGCValue)
            pp.setGcValue(childId)
            pp.setGcValue(parentId)
          }
        )

      case None =>
        val query = sql
          .deleteFrom(relationTable(relation))
          .where(condition)

        deleteToDBIO(query)(setParams = { pp =>
          pp.setGcValue(childId)
          pp.setGcValue(parentId)
        })
    }
  }

  def deleteRelationRowByParentId(relationField: RelationField, parentId: IdGCValue): DBIO[_] = {
    val relation  = relationField.relation
    val condition = relationColumn(relation, relationField.relationSide).equal(placeHolder)
    relation.inlineManifestation match {
      case Some(manifestation) =>
        val query = sql
          .update(relationTable(relation))
          .set(inlineRelationColumn(relation, manifestation), placeHolder)
          .where(condition)

        updateToDBIO(query)(
          setParams = { pp =>
            pp.setGcValue(NullGCValue)
            pp.setGcValue(parentId)
          }
        )

      case None =>
        val query = sql
          .deleteFrom(relationTable(relation))
          .where(condition)

        deleteToDBIO(query)(setParams = _.setGcValue(parentId))
    }
  }
}
