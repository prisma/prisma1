package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.{QueryArguments, RelationNode, ResolverResult}
import com.prisma.shared.models.Relation

trait RelationQueries extends BuilderBase with FilterConditionBuilder with OrderByClauseBuilder with LimitClauseBuilder {
  import slickDatabase.profile.api._

  def getRelationNodes(
      relation: Relation,
      args: Option[QueryArguments]
  ): DBIO[ResolverResult[RelationNode]] = {

    lazy val query = {
      val aliasedTable = relationTable(relation).as(topLevelAlias)
      val condition    = buildConditionForFilter(args.flatMap(_.filter))
      val order        = orderByForRelation(relation, topLevelAlias, args)
      val limit        = limitClause(args)

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

    queryToDBIO(query)(
      setParams = pp => SetParams.setQueryArgs(pp, args),
      readResult = rs => {
        val result = rs.readWith(readRelation(relation))
        ResolverResult(args, result)
      }
    )
  }
}
