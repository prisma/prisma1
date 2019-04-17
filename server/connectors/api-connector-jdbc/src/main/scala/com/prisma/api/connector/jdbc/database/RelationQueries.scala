package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.{QueryArguments, RelationNode, ResolverResult}
import com.prisma.api.helpers.LimitClauseHelper
import com.prisma.shared.models.Relation

trait RelationQueries extends BuilderBase with FilterConditionBuilder with OrderByClauseBuilder with LimitClauseBuilder {
  import slickDatabase.profile.api._

  def getRelationNodes(
      relation: Relation,
      args: QueryArguments
  ): DBIO[ResolverResult[RelationNode]] = {

    lazy val query = if (relation.isRelationTable) {
      val aliasedTable = relationTable(relation).as(topLevelAlias)
      val order        = orderByForRelation(relation, topLevelAlias, args)
      val skipAndLimit = LimitClauseHelper.skipAndLimitValues(args)

      val base = sql
        .select()
        .from(aliasedTable)
        .orderBy(order: _*)
        .offset(intDummy)

      skipAndLimit.limit match {
        case Some(_) => base.limit(intDummy)
        case None    => base
      }
    } else {
      val aliasedTable      = relationTable(relation).as(topLevelAlias)
      val order             = orderByForRelation(relation, topLevelAlias, args)
      val skipAndLimit      = LimitClauseHelper.skipAndLimitValues(args)
      val isInlinedInModelA = relation.modelAField.relationIsInlinedInParent

      val conditionField = if (isInlinedInModelA) aliasColumn(relation.modelAField) else aliasColumn(relation.modelBField)

      val fieldA = if (isInlinedInModelA) modelIdColumn(topLevelAlias, relation.modelA) else aliasColumn(relation.modelBField)
      val fieldB = if (isInlinedInModelA) aliasColumn(relation.modelAField) else modelIdColumn(topLevelAlias, relation.modelB)

      val base = sql
        .select(fieldA.as("A"), fieldB.as("B"))
        .from(aliasedTable)
        .where(conditionField.isNotNull)
        .orderBy(order: _*)
        .offset(intDummy)

      skipAndLimit.limit match {
        case Some(_) => base.limit(intDummy)
        case None    => base
      }
    }

    queryToDBIO(query)(
      setParams = pp => SetParams.setQueryArgs(pp, args),
      readResult = rs => {
        val result = rs.readWith(readRelation(relation))
        ResolverResult(args, result)
      }
    )
  }
}
