package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector._
import com.prisma.gc_values.{GCValue, IdGCValue}
import com.prisma.shared.models._
import org.jooq.impl.DSL._

case class JooqRelationQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    relation: Relation,
    queryArguments: Option[QueryArguments]
) extends AllBuilders {

  lazy val queryString: String = {
    val aliasedTable = table(name(schemaName, relation.relationTableName)).as(topLevelAlias)
    val condition    = buildConditionForFilter(queryArguments.flatMap(_.filter))
    val order        = orderByForRelation(relation, topLevelAlias, queryArguments)
    val limit        = limitClause(queryArguments)

    val base = sql
      .select()
      .from(aliasedTable)
      .where(condition)
      .orderBy(order: _*)

    val finalQuery = limit match {
      case Some(_) => base.limit(intDummy).offset(intDummy)
      case None    => base
    }

    finalQuery.getSQL
  }
}

case class JooqCountQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    tableName: String,
    filter: Option[Filter]
) extends AllBuilders {

  lazy val queryString: String = {
    val aliasedTable = table(name(schemaName, tableName)).as(topLevelAlias)
    val condition    = buildConditionForFilter(filter)

    val query = sql
      .selectCount()
      .from(aliasedTable)
      .where(condition)

    query.getSQL
  }
}

case class JooqScalarListQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    field: ScalarField,
    queryArguments: Option[QueryArguments]
) extends AllBuilders {
  require(field.isList, "This must be called only with scalar list fields")

  val tableName = s"${field.model.dbName}_${field.dbName}"
  lazy val queryString: String = {
    val aliasedTable = table(name(schemaName, tableName)).as(topLevelAlias)
    val condition    = buildConditionForFilter(queryArguments.flatMap(_.filter))
    val order        = orderByForScalarListField(topLevelAlias, queryArguments)
    val limit        = limitClause(queryArguments)

    val base = sql
      .select()
      .from(aliasedTable)
      .where(condition)
      .orderBy(order: _*)

    val finalQuery = limit match {
      case Some(_) => base.limit(intDummy).offset(intDummy)
      case None    => base
    }

    finalQuery.getSQL
  }
}

case class JooqScalarListByUniquesQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    scalarField: ScalarField,
    nodeIds: Vector[GCValue]
) extends AllBuilders {
  require(scalarField.isList, "This must be called only with scalar list fields")

  val tableName = s"${scalarField.model.dbName}_${scalarField.dbName}"
  lazy val queryString: String = {
    val nodeIdField = field(name(schemaName, tableName, nodeIdFieldName))

    val condition = nodeIdField.in(Vector.fill(nodeIds.length) { stringDummy }: _*)
    val query = sql
      .select(nodeIdField, field(name(positionFieldName)), field(name(valueFieldName)))
      .from(table(name(schemaName, tableName)))
      .where(condition)

    query.getSQL
  }
}

case class JooqRelatedModelsQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    fromField: RelationField,
    queryArguments: Option[QueryArguments],
    relatedNodeIds: Vector[IdGCValue]
) extends AllBuilders {

  val relation                        = fromField.relation
  val relatedModel                    = fromField.relatedModel_!
  val modelTable                      = relatedModel.dbName
  val relationTableName               = fromField.relation.relationTableName
  val modelRelationSideColumn         = relation.columnForRelationSide(fromField.relationSide)
  val oppositeModelRelationSideColumn = relation.columnForRelationSide(fromField.oppositeRelationSide)
  val aColumn                         = relation.modelAColumn
  val bColumn                         = relation.modelBColumn
  val secondaryOrderByForPagination   = if (fromField.oppositeRelationSide == RelationSide.A) aSideAlias else bSideAlias

  val aliasedTable            = table(name(schemaName, modelTable)).as(topLevelAlias)
  val relationTable           = table(name(schemaName, relationTableName)).as(relationTableAlias)
  val relatedNodesCondition   = field(name(relationTableAlias, modelRelationSideColumn)).in(placeHolders(relatedNodeIds))
  val queryArgumentsCondition = buildConditionForFilter(queryArguments.flatMap(_.filter))

  val base = sql
    .select(aliasedTable.asterisk(), field(name(relationTableAlias, aColumn)).as(aSideAlias), field(name(relationTableAlias, bColumn)).as(bSideAlias))
    .from(aliasedTable)
    .innerJoin(relationTable)
    .on(aliasColumn(relatedModel.dbNameOfIdField_!).eq(field(name(relationTableAlias, oppositeModelRelationSideColumn))))

  lazy val queryStringWithPagination: String = {
    val order           = orderByInternal(baseTableAlias, baseTableAlias, secondaryOrderByForPagination, queryArguments)
    val cursorCondition = JooqWhereClauseBuilder(slickDatabase, schemaName).buildCursorCondition(queryArguments, relatedModel)

    val aliasedBase = base.where(relatedNodesCondition, queryArgumentsCondition, cursorCondition).asTable().as(baseTableAlias)

    val rowNumberPart = rowNumber().over().partitionBy(aliasedBase.field(aSideAlias)).orderBy(order: _*).as(rowNumberAlias)

    val withRowNumbers = select(rowNumberPart, aliasedBase.asterisk()).from(aliasedBase).asTable().as(rowNumberTableAlias)

    val limitCondition = rowNumberPart.between(intDummy).and(intDummy)

    val withPagination = sql
      .select(withRowNumbers.asterisk())
      .from(withRowNumbers)
      .where(limitCondition)

    withPagination.getSQL
  }

  lazy val mysqlHack = {
    val relatedNodeCondition = field(name(relationTableAlias, modelRelationSideColumn)).equal(placeHolder)
    val order                = orderByInternal2(secondaryOrderByForPagination, queryArguments)
    val cursorCondition      = JooqWhereClauseBuilder(slickDatabase, schemaName).buildCursorCondition(queryArguments, relatedModel)

    val singleQuery =
      base
        .where(relatedNodeCondition, queryArgumentsCondition, cursorCondition)
        .orderBy(order: _*)
        .limit(intDummy)
        .offset(intDummy)

    singleQuery
  }

  lazy val queryStringWithoutPagination: String = {
    val order = orderByInternal(topLevelAlias, relationTableAlias, oppositeModelRelationSideColumn, queryArguments)
    val withoutPagination = base
      .where(relatedNodesCondition, queryArgumentsCondition)
      .orderBy(order: _*)

    withoutPagination.getSQL
  }
}

case class JooqModelQueryBuilder(
    slickDatabase: SlickDatabase,
    schemaName: String,
    model: Model,
    queryArguments: Option[QueryArguments]
) extends AllBuilders {

  lazy val queryString: String = {
    val condition       = buildConditionForFilter(queryArguments.flatMap(_.filter)))
    val cursorCondition = JooqWhereClauseBuilder(slickDatabase, schemaName).buildCursorCondition(queryArguments, model)
    val order           = orderByForModel(model, topLevelAlias, queryArguments)
    val limit           = limitClause(queryArguments)

    val aliasedTable = table(name(schemaName, model.dbName)).as(topLevelAlias)

    val base = sql
      .select()
      .from(aliasedTable)
      .where(condition, cursorCondition)
      .orderBy(order: _*)

    val finalQuery = limit match {
      case Some(_) => base.limit(intDummy).offset(intDummy)
      case None    => base
    }

    finalQuery.getSQL
  }
}
