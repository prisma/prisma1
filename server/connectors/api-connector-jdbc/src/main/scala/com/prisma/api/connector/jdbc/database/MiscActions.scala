package com.prisma.api.connector.jdbc.database

import com.prisma.shared.models.Project

trait MiscActions extends BuilderBase {
  import slickDatabase.profile.api._

  def truncateTables(project: Project): DBIO[_] = {
    val relationTables = project.relations.map(relationTable)
    val modelTables    = project.models.map(modelTable)
    val listTables     = project.models.flatMap(model => model.scalarListFields.map(scalarListTable))
    val actions = (relationTables ++ listTables ++ Vector(relayTable) ++ modelTables).map { table =>
      if (isMySql) {
        truncateToDBIO(sql.truncate(table))
      } else {
        truncateToDBIO(sql.truncate(table).cascade())
      }
    }
    val truncatesAction = DBIO.sequence(actions)

    def disableForeignKeyChecks = SimpleDBIO { ctx =>
      val ps = ctx.connection.prepareStatement("SET FOREIGN_KEY_CHECKS=0")
      ps.executeUpdate()
    }
    def enableForeignKeyChecks = SimpleDBIO { ctx =>
      val ps = ctx.connection.prepareStatement("SET FOREIGN_KEY_CHECKS=1")
      ps.executeUpdate()
    }

    if (isMySql) {
      DBIO.seq(disableForeignKeyChecks, truncatesAction, enableForeignKeyChecks)
    } else {
      truncatesAction
    }
  }
}
