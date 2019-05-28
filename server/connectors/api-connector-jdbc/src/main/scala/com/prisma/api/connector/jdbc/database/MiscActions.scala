package com.prisma.api.connector.jdbc.database

import com.prisma.shared.models.Project
import com.prisma.utils.boolean.BooleanUtils

trait MiscActions extends BuilderBase with BooleanUtils {
  import slickDatabase.profile.api._

  def truncateTables(project: Project): DBIO[_] = {
    val relationTables = project.relations.map(relationTable)
    val modelTables    = project.models.map(modelTable)
    val listTables     = project.models.flatMap(model => model.scalarListFields.map(scalarListTable))

    val actions = (relationTables ++ listTables ++ modelTables).map {
      case table if isMySql => truncateToDBIO(sql.truncate(table))
      case table            => truncateToDBIO(sql.truncate(table).cascade())
    }

    lazy val disableForeignKeyChecks = SimpleDBIO { ctx =>
      val ps = ctx.connection.prepareStatement("SET FOREIGN_KEY_CHECKS=0")
      ps.executeUpdate()
    }
    lazy val enableForeignKeyChecks = SimpleDBIO { ctx =>
      val ps = ctx.connection.prepareStatement("SET FOREIGN_KEY_CHECKS=1")
      ps.executeUpdate()
    }

    val truncatesAction = DBIO.sequence(actions)

    if (isMySql) DBIO.seq(disableForeignKeyChecks, truncatesAction, enableForeignKeyChecks) else truncatesAction
  }
}
