package cool.graph.system.database.finder

import cool.graph.system.database.tables.Tables
import cool.graph.shared.models.ProjectDatabase
import cool.graph.shared.models.Region.Region
import cool.graph.system.database.DbToModelMapper
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.Future

object ProjectDatabaseFinder {
  import cool.graph.system.database.tables.ProjectTable.regionMapper
  import scala.concurrent.ExecutionContext.Implicits.global

  def forId(id: String)(internalDatabase: DatabaseDef): Future[Option[ProjectDatabase]] = {
    val query = Tables.ProjectDatabases.filter(_.id === id).result.headOption
    internalDatabase.run(query).map { dbResult: Option[cool.graph.system.database.tables.ProjectDatabase] =>
      dbResult.map(DbToModelMapper.createProjectDatabase)
    }
  }

  def defaultForRegion(region: Region)(internalDatabase: DatabaseDef): Future[Option[ProjectDatabase]] = {
    val query =
      Tables.ProjectDatabases.filter(pdb => pdb.region === region && pdb.isDefaultForRegion).result.headOption
    internalDatabase.run(query).map { dbResult: Option[cool.graph.system.database.tables.ProjectDatabase] =>
      dbResult.map(DbToModelMapper.createProjectDatabase)
    }
  }
}
