package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector._
import com.prisma.api.helpers.LimitClauseHelper
import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models._
import org.jooq.impl.DSL._

case class RelatedModelsQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    fromField: RelationField,
    queryArguments: QueryArguments,
    relatedNodeIds: Vector[IdGCValue],
    selectedFields: SelectedFields
) extends BuilderBase
    with FilterConditionBuilder
    with OrderByClauseBuilder
    with CursorConditionBuilder
    with LimitClauseBuilder {

  val relation                        = fromField.relation
  val relatedModel                    = fromField.relatedModel_!
  val modelRelationSideColumn         = relation.columnForRelationSide(fromField.relationSide)
  val oppositeModelRelationSideColumn = relation.columnForRelationSide(fromField.oppositeRelationSide)
  val aColumn                         = relation.modelAColumn
  val bColumn                         = relation.modelBColumn
  val secondaryOrderByForPagination   = relatedModelAlias

  val aliasedTable            = modelTable(relatedModel).as(topLevelAlias)
  val relationTable2          = relationTable(relation).as(relationTableAlias)
  val relatedNodesCondition   = field(name(relationTableAlias, modelRelationSideColumn)).in(placeHolders(relatedNodeIds))
  val queryArgumentsCondition = buildConditionForFilter(queryArguments.filter)

  val relatedModelSide = relation.columnForRelationSide(fromField.oppositeRelationSide)
  val parentModelSide  = relation.columnForRelationSide(fromField.relationSide)

  val selectedJooqFields = selectedFields.scalarDbFields.map(aliasColumn).toVector :+
    field(name(relationTableAlias, relatedModelSide)).as(relatedModelAlias) :+
    field(name(relationTableAlias, parentModelSide)).as(parentModelAlias)

  val base = sql
    .select(selectedJooqFields: _*)
    .from(aliasedTable)
    .innerJoin(relationTable2)
    .on(aliasColumn(relatedModel.dbNameOfIdField_!).eq(field(name(relationTableAlias, oppositeModelRelationSideColumn))))

  val cursorCondition = buildCursorCondition(queryArguments, relatedModel)

  lazy val queryWithPagination = {
    val order          = orderByInternalWithAliases(baseTableAlias, baseTableAlias, secondaryOrderByForPagination, queryArguments)
    val aliasedBase    = base.where(relatedNodesCondition, queryArgumentsCondition, cursorCondition).asTable().as(baseTableAlias)
    val rowNumberPart  = rowNumber().over().partitionBy(field(name(baseTableAlias, parentModelAlias))).orderBy(order: _*).as(rowNumberAlias)
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
    val skipAndLimit         = LimitClauseHelper.skipAndLimitValues(queryArguments)

    val tmp = base
      .where(relatedNodeCondition, queryArgumentsCondition, cursorCondition)
      .orderBy(order: _*)
      .offset(intDummy)

    skipAndLimit.limit match {
      case Some(_) => tmp.limit(intDummy)
      case None    => tmp
    }
  }

  lazy val queryWithoutPagination = {
    val order = orderByInternalWithAliases(topLevelAlias, relationTableAlias, oppositeModelRelationSideColumn, queryArguments)
    base
      .where(relatedNodesCondition, queryArgumentsCondition, cursorCondition)
      .orderBy(order: _*)
  }
}
