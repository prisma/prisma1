package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.{QueryArguments, RelationNode, ResolverResult, ScalarFilter}
import com.prisma.api.helpers.LimitClauseHelper
import com.prisma.shared.models.Relation
import com.typesafe.sslconfig.ssl.NotEqual

trait RelationQueries extends BuilderBase with FilterConditionBuilder with OrderByClauseBuilder with LimitClauseBuilder {
  import slickDatabase.profile.api._

  def getRelationNodes(
      relation: Relation,
      args: QueryArguments
  ): DBIO[ResolverResult[RelationNode]] = {

    //needs special case for inline relations. then only the ones without null entries should be retrieved

    lazy val query = if (!relation.isInlineRelation) {
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
    } else if (relation.modelAField.relationIsInlinedInParent) {
      val aliasedTable = relationTable(relation).as(topLevelAlias)
      val order        = orderByForRelation(relation, topLevelAlias, args)
      val skipAndLimit = LimitClauseHelper.skipAndLimitValues(args)

      val base = sql
        .select(modelIdColumn(topLevelAlias, relation.modelA).as("A"), aliasColumn(relation.modelAField).as("B"))
        .from(aliasedTable)
        .where(aliasColumn(relation.modelAField).isNotNull)
        .orderBy(order: _*)
        .offset(intDummy)

      skipAndLimit.limit match {
        case Some(_) => base.limit(intDummy)
        case None    => base
      }
    } else {
      val aliasedTable = relationTable(relation).as(topLevelAlias)
      val order        = orderByForRelation(relation, topLevelAlias, args)
      val skipAndLimit = LimitClauseHelper.skipAndLimitValues(args)

      val base = sql
        .select(aliasColumn(relation.modelBField).as("A"), modelIdColumn(topLevelAlias, relation.modelB).as("B"))
        .from(aliasedTable)
        .where(aliasColumn(relation.modelBField).isNotNull)
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
