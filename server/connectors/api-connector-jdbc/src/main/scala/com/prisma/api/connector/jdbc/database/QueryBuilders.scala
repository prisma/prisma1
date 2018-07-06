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

  lazy val queryWithPagination = {
    val order           = orderByInternal(baseTableAlias, baseTableAlias, secondaryOrderByForPagination, queryArguments)
    val cursorCondition = buildCursorCondition(queryArguments, relatedModel)

    val aliasedBase = base.where(relatedNodesCondition, queryArgumentsCondition, cursorCondition).asTable(baseTableAlias)

    val rowNumberPart = rowNumber().over().partitionBy(aliasedBase.field(aSideAlias)).orderBy(order: _*).as(rowNumberAlias)

    val withRowNumbers = select(rowNumberPart, aliasedBase.asterisk()).from(aliasedBase).asTable(rowNumberTableAlias)

    val limitCondition = rowNumberPart.between(intDummy).and(intDummy)

    sql
      .select(withRowNumbers.asterisk())
      .from(withRowNumbers)
      .where(limitCondition)
  }

  lazy val mysqlHack = {

    val order           = orderByInternal(baseTableAlias, baseTableAlias, secondaryOrderByForPagination, queryArguments)
    val cursorCondition = buildCursorCondition(queryArguments, relatedModel)

    val aliasedBase   = base.where(relatedNodesCondition, queryArgumentsCondition, cursorCondition).asTable(baseTableAlias)
    val rowNumberPart = field("""@id_rank := IF(@current_id = `prismaBaseTableAlias`.`__Relation__A`, @id_rank + 1, 1)""", Integer.TYPE).as(rowNumberAlias)
    val currentId     = field("""@current_id := `prismaBaseTableAlias`.`__Relation__A`""").as("dummy")

    val withRowNumbers =
      select(rowNumberPart, currentId, aliasedBase.asterisk())
        .from(aliasedBase)
        .orderBy(order: _*)
        .asTable(rowNumberTableAlias)

    val limitCondition = rowNumberPart.between(intDummy).and(intDummy)

    sql
      .select(withRowNumbers.asterisk())
      .from(withRowNumbers)
      .where(limitCondition)

  }

  lazy val mysqlHack2 = {

    val order           = orderByInternal2(secondaryOrderByForPagination, queryArguments)
    val cursorCondition = buildCursorCondition(queryArguments, relatedModel)

    val aliasedBase = base
      .where(relatedNodesCondition, queryArgumentsCondition, cursorCondition)
      .orderBy(order: _*)
      .asTable(baseTableAlias)

    val rank    = field("""(@id_rank := IF(@current_id = `prismaBaseTableAlias`.`__Relation__A`, @id_rank + 1, 1))""").isNotNull
    val current = field("""(@current_id := `prismaBaseTableAlias`.`__Relation__A`)""").isNotNull
    val between = field("""@id_rank""", Integer.TYPE).between(intDummy).and(intDummy)

    sql
      .select(field("""@id_rank"""), aliasedBase.asterisk())
      .from(aliasedBase)
      .where(rank, current, between)
  }

  lazy val queryWithoutPagination = {
    val order = orderByInternal(topLevelAlias, relationTableAlias, oppositeModelRelationSideColumn, queryArguments)
    base
      .where(relatedNodesCondition, queryArgumentsCondition)
      .orderBy(order: _*)
  }
}
