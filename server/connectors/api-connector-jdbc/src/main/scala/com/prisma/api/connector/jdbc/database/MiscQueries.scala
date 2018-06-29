package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.Filter
import slick.jdbc.PositionedParameters

trait MiscQueries extends BuilderBase {
  import slickDatabase.profile.api._

  def countAllFromTable(table: String, whereFilter: Option[Filter]): DBIO[Int] = {
    val builder = CountQueryBuilder(slickDatabase, schemaName, table, whereFilter)
    queryToDBIO(builder.query)(
      setParams = pp => SetParams.setFilter(pp, whereFilter),
      readResult = rs => {
        rs.next()
        rs.getInt(1)
      }
    )
  }
}
