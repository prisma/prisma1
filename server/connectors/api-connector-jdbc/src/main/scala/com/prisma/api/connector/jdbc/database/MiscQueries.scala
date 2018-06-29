package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.Filter
import slick.jdbc.PositionedParameters

trait MiscQueries extends BuilderBase {
  import slickDatabase.profile.api._

  def countAllFromTable(table: String, whereFilter: Option[Filter]): DBIO[Int] = {
    SimpleDBIO[Int] { ctx =>
      val builder = CountQueryBuilder(slickDatabase, schemaName, table, whereFilter)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      SetParams.setFilter(new PositionedParameters(ps), whereFilter)
      val rs = ps.executeQuery()
      rs.next()
      rs.getInt(1)
    }
  }
}
