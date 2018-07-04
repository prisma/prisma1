package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector._
import com.prisma.gc_values.{GCValue, IdGCValue}
import com.prisma.shared.models._
import org.jooq.impl.DSL._

case class RelationQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    relation: Relation,
    queryArguments: Option[QueryArguments]
) extends BuilderBase
    with FilterConditionBuilder
    with OrderByClauseBuilder
    with LimitClauseBuilder {

  lazy val query = {
    val aliasedTable = relationTable(relation).as(topLevelAlias)
    val condition    = buildConditionForFilter(queryArguments.flatMap(_.filter))
    val order        = orderByForRelation(relation, topLevelAlias, queryArguments)
    val limit        = limitClause(queryArguments)

    val base = sql
      .select()
      .from(aliasedTable)
      .where(condition)
      .orderBy(order: _*)

    limit match {
      case Some(_) => base.limit(intDummy).offset(intDummy)
      case None    => base
    }
  }
}

case class CountQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    tableName: String,
    filter: Option[Filter]
) extends BuilderBase
    with FilterConditionBuilder {

  lazy val query = {
    val aliasedTable = table(name(schemaName, tableName)).as(topLevelAlias)
    val condition    = buildConditionForFilter(filter)

    sql
      .selectCount()
      .from(aliasedTable)
      .where(condition)
  }
}

case class ScalarListQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    field: ScalarField,
    queryArguments: Option[QueryArguments]
) extends BuilderBase
    with FilterConditionBuilder
    with OrderByClauseBuilder
    with LimitClauseBuilder {
  require(field.isList, "This must be called only with scalar list fields")

  lazy val query = {
    val condition = buildConditionForFilter(queryArguments.flatMap(_.filter))
    val order     = orderByForScalarListField(topLevelAlias, queryArguments)
    val limit     = limitClause(queryArguments)

    val base = sql
      .select()
      .from(scalarListTable(field).as(topLevelAlias))
      .where(condition)
      .orderBy(order: _*)

    limit match {
      case Some(_) => base.limit(intDummy).offset(intDummy)
      case None    => base
    }
  }
}

case class ScalarListByUniquesQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    scalarField: ScalarField,
    nodeIds: Vector[GCValue]
) extends BuilderBase {
  require(scalarField.isList, "This must be called only with scalar list fields")

  lazy val query = {
    val nodeIdField   = scalarListColumn(scalarField, nodeIdFieldName)
    val positionField = scalarListColumn(scalarField, positionFieldName)
    val valueField    = scalarListColumn(scalarField, valueFieldName)
    val condition     = nodeIdField.in(Vector.fill(nodeIds.length) { stringDummy }: _*)

    sql
      .select(nodeIdField, positionField, valueField)
      .from(scalarListTable(scalarField))
      .where(condition)

  }
}

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
    val order                = orderByInternal2(secondaryOrderByForPagination, queryArguments)
    val cursorCondition      = buildCursorCondition(queryArguments, relatedModel)

    base
      .where(relatedNodeCondition, queryArgumentsCondition, cursorCondition)
      .orderBy(order: _*)
      .limit(intDummy)
      .offset(intDummy)
  }

  lazy val queryWithoutPagination = {
    val order = orderByInternal(topLevelAlias, relationTableAlias, oppositeModelRelationSideColumn, queryArguments)
    base
      .where(relatedNodesCondition, queryArgumentsCondition)
      .orderBy(order: _*)
  }
}

case class ModelQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    model: Model,
    queryArguments: Option[QueryArguments]
) extends BuilderBase
    with FilterConditionBuilder
    with CursorConditionBuilder
    with OrderByClauseBuilder
    with LimitClauseBuilder {

  lazy val query = {
    val condition       = buildConditionForFilter(queryArguments.flatMap(_.filter))
    val cursorCondition = buildCursorCondition(queryArguments, model)
    val order           = orderByForModel(model, topLevelAlias, queryArguments)
    val limit           = limitClause(queryArguments)

    val base = sql
      .select()
      .from(modelTable(model).as(topLevelAlias))
      .where(condition, cursorCondition)
      .orderBy(order: _*)

    limit match {
      case Some(_) => base.limit(intDummy).offset(intDummy)
      case None    => base
    }
  }
}
