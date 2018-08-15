package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector._
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models._
import org.jooq.impl.DSL._

case class RelatedModelsQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    fromField: RelationField,
    queryArguments: Option[QueryArguments],
    relatedNodeIds: Vector[IdGCValue]
) extends BuilderBase
    with FilterConditionBuilder
    with OrderByClauseBuilder
    with CursorConditionBuilder {

  val relation                        = fromField.relation
  val relatedModel                    = fromField.relatedModel_!
  val modelRelationSideColumn         = relation.columnForRelationSide(fromField.relationSide)
  val oppositeModelRelationSideColumn = relation.columnForRelationSide(fromField.oppositeRelationSide)
  val aColumn                         = relation.modelAColumn
  val bColumn                         = relation.modelBColumn
  val secondaryOrderByForPagination   = if (fromField.oppositeRelationSide == RelationSide.A) aSideAlias else bSideAlias

  val aliasedTable            = modelTable(relatedModel).as(topLevelAlias)
  val relationTable2          = relationTable(relation).as(relationTableAlias)
  val relatedNodesCondition   = field(name(relationTableAlias, modelRelationSideColumn)).in(placeHolders(relatedNodeIds))
  val queryArgumentsCondition = buildConditionForFilter(queryArguments.flatMap(_.filter))

  val base = sql
    .select(aliasedTable.asterisk(), field(name(relationTableAlias, aColumn)).as(aSideAlias), field(name(relationTableAlias, bColumn)).as(bSideAlias))
    .from(aliasedTable)
    .innerJoin(relationTable2)
    .on(aliasColumn(relatedModel.dbNameOfIdField_!).eq(field(name(relationTableAlias, oppositeModelRelationSideColumn))))
  val cursorCondition = buildCursorCondition(queryArguments, relatedModel)

  lazy val queryWithPagination = {
    val order = orderByInternalWithAliases(baseTableAlias, baseTableAlias, secondaryOrderByForPagination, queryArguments)

    val aliasedBase = base.where(relatedNodesCondition, queryArgumentsCondition, cursorCondition).asTable().as(baseTableAlias)

    val rowNumberPart = rowNumber().over().partitionBy(aliasedBase.field(aSideAlias)).orderBy(order: _*).as(rowNumberAlias)

    val withRowNumbers = select(rowNumberPart, aliasedBase.asterisk()).from(aliasedBase).asTable().as(rowNumberTableAlias)

    val limitCondition = rowNumberPart.between(intDummy).and(intDummy)

    sql
      .select(withRowNumbers.asterisk())
      .from(withRowNumbers)
      .where(limitCondition)
  }

  lazy val mysqlHack = {
    val relatedNodeCondition = field(name(relationTableAlias, modelRelationSideColumn)).equal(placeHolder)
    val order                = orderByInternal(secondaryOrderByForPagination, queryArguments)

    base
      .where(relatedNodeCondition, queryArgumentsCondition, cursorCondition)
      .orderBy(order: _*)
      .limit(intDummy)
      .offset(intDummy)
  }

  lazy val queryWithoutPagination = {
    val order = orderByInternalWithAliases(topLevelAlias, relationTableAlias, oppositeModelRelationSideColumn, queryArguments)
    base
      .where(relatedNodesCondition, queryArgumentsCondition, cursorCondition)
      .orderBy(order: _*)
  }
}
