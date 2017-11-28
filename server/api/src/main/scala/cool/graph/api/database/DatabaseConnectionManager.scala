package cool.graph.api.database

import com.typesafe.config.{Config}
import cool.graph.shared.models.{Project, ProjectDatabase, Region}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

case class Databases(master: DatabaseDef, readOnly: DatabaseDef)

case class DatabaseConnectionManager(databases: Map[String, Databases]) {

  def getDbForProject(project: Project): Databases = getDbForProjectDatabase(project.projectDatabase)

  def getDbForProjectDatabase(projectDatabase: ProjectDatabase): Databases = {
    databases.get(projectDatabase.name) match {
      case None =>
        sys.error(s"This service is not configured to access Client Db with name [${projectDatabase.name}]")
      case Some(db) => db
    }
  }
}

object DatabaseConnectionManager {
  val singleConfigRoot    = "clientDatabases"
  val allConfigRoot       = "allClientDatabases"
  val awsRegionConfigProp = "awsRegion"

  def initializeForSingleRegion(config: Config): DatabaseConnectionManager = {
    import scala.collection.JavaConversions._
    config.resolve()

    val databasesMap = for {
      (dbName, _) <- config.getObject(singleConfigRoot)
    } yield {
      val readOnlyPath    = s"$singleConfigRoot.$dbName.readonly"
      val masterDb        = Database.forConfig(s"$singleConfigRoot.$dbName.master", config)
      lazy val readOnlyDb = Database.forConfig(readOnlyPath, config)

      val dbs = Databases(
        master = masterDb,
        readOnly = if (config.hasPath(readOnlyPath)) readOnlyDb else masterDb
      )

      dbName -> dbs
    }

    DatabaseConnectionManager(databases = databasesMap.toMap)
  }

}
