package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.{QueryArguments, RelationNode, ResolverResult}
import com.prisma.shared.models.Relation

trait RelationQueries extends BuilderBase {
  import slickDatabase.profile.api._

  def getRelationNodes(
      relation: Relation,
      args: Option[QueryArguments]
  ): DBIO[ResolverResult[RelationNode]] = {
    val builder = RelationQueryBuilder(slickDatabase, schemaName, relation, args)
    queryToDBIO(builder.query)(
      setParams = pp => SetParams.setQueryArgs(pp, args),
      readResult = rs => {
        val result = rs.readWith(readRelation(relation))
        ResolverResult(args, result)
      }
    )
  }
}
