package com.prisma.api.connector.jdbc.database

import java.sql.ResultSet

import com.prisma.api.connector.{QueryArguments, RelationNode, ResolverResult}
import com.prisma.shared.models.Relation

trait RelationQueries extends BuilderBase {
  import slickDatabase.profile.api._

  def selectAllFromRelationTable(
      relation: Relation,
      args: Option[QueryArguments]
  ): DBIO[ResolverResult[RelationNode]] = {

    SimpleDBIO[ResolverResult[RelationNode]] { ctx =>
      val builder = RelationQueryBuilder(slickDatabase, schemaName, relation, args)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      SetParams.setQueryArgs(ps, args)
      val rs: ResultSet = ps.executeQuery()
      val result        = rs.as(readRelation(relation))
      ResolverResult(result)
    }
  }
}
