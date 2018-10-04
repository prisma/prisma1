package com.prisma.api.connector.jdbc.database

import com.prisma.gc_values.{IdGCValue, NullGCValue}
import com.prisma.shared.models.{RelationField, RelationSide}
import cool.graph.cuid.Cuid

trait RelationActions extends BuilderBase {
  import slickDatabase.profile.api._

  def createRelation(relationField: RelationField, parentId: IdGCValue, childId: IdGCValue): DBIO[_] = {
    val relation = relationField.relation

    if (relation.isInlineRelation) {
      val inlineManifestation  = relation.inlineManifestation.get
      val referencingColumn    = inlineManifestation.referencingColumn
      val childModel           = relationField.relatedModel_!
      val parentModel          = relationField.model
      val childWhereCondition  = idField(childModel).equal(placeHolder)
      val parentWhereCondition = idField(parentModel).equal(placeHolder)

      val (idToLinkTo, idToUpdate, rowToUpdateCondition) = () match {
        case _ if relation.isSelfRelation && relationField.relationSide == RelationSide.B => (parentId, childId, parentWhereCondition)
        case _ if relation.isSelfRelation && relationField.relationSide == RelationSide.A => (childId, parentId, childWhereCondition)
        case _ if inlineManifestation.inTableOfModelId == childModel.name                 => (parentId, childId, childWhereCondition)
        case _ if inlineManifestation.inTableOfModelId == parentModel.name                => (childId, parentId, parentWhereCondition)
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

    } else if (relation.hasManifestation) {
      val query = sql
        .insertInto(relationTable(relation))
        .columns(
          relationColumn(relation, relationField.relationSide),
          relationColumn(relation, relationField.oppositeRelationSide)
        )
        .values(placeHolder, placeHolder)

      insertToDBIO(query)(
        setParams = { pp =>
          pp.setGcValue(parentId)
          pp.setGcValue(childId)
        }
      )

    } else {
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
          pp.setString(Cuid.createCuid())
          pp.setGcValue(parentId)
          pp.setGcValue(childId)
        }
      )
    }
  }

  def deleteRelationRowByChildId(relationField: RelationField, childId: IdGCValue): DBIO[Unit] = {
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

  def deleteRelationRowByChildIdAndParentId(relationField: RelationField, childId: IdGCValue, parentId: IdGCValue): DBIO[Unit] = {
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

  def deleteRelationRowByParentId(relationField: RelationField, parentId: IdGCValue): DBIO[Unit] = {
    assert(!relationField.isList)
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
