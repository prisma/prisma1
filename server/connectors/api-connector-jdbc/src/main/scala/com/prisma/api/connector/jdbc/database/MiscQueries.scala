package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.Filter
import org.jooq.impl.DSL.{name, table}

trait MiscQueries extends BuilderBase with FilterConditionBuilder {
  import slickDatabase.profile.api._

  def countAllFromTable(tableName: String, whereFilter: Option[Filter]): DBIO[Int] = {

    lazy val query = {
      val aliasedTable = table(name(schemaName, tableName)).as(topLevelAlias)
      val condition    = buildConditionForFilter(whereFilter)

      sql
        .selectCount()
        .from(aliasedTable)
        .where(condition)
    }

    queryToDBIO(query)(
      setParams = pp => SetParams.setFilter(pp, whereFilter),
      readResult = rs => {
        rs.next()
        rs.getInt(1)
      }
    )
  }
}
