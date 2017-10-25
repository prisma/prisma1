package cool.graph.shared.database

import com.typesafe.config.{Config, ConfigObject}
import cool.graph.shared.models.Region.Region
import cool.graph.shared.models.{Project, ProjectDatabase, Region}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

object InternalAndProjectDbs {
  def apply(internal: InternalDatabase, client: Databases): InternalAndProjectDbs = {
    InternalAndProjectDbs(internal, Some(client))
  }
}
case class InternalAndProjectDbs(internal: InternalDatabase, client: Option[Databases] = None)
case class Databases(master: DatabaseDef, readOnly: DatabaseDef)
case class InternalDatabase(databaseDef: DatabaseDef)

/**
  * Unfortunately the system api needs access to the client db in each region.
  * Therefore we use this class to select the correct db for a project.
  * As the system and client apis use the same DataResolver we also use this intermediary class in client api,
  * even though they are only configured with access to the local client db.
  */
case class ProjectDatabaseRef(region: Region, name: String)

case class GlobalDatabaseManager(currentRegion: Region, databases: Map[ProjectDatabaseRef, Databases]) {

  def getDbForProject(project: Project): Databases = getDbForProjectDatabase(project.projectDatabase)

  def getDbForProjectDatabase(projectDatabase: ProjectDatabase): Databases = {
    val projectDbRef = ProjectDatabaseRef(projectDatabase.region, projectDatabase.name)
    databases.get(projectDbRef) match {
      case None =>
        sys.error(s"This service is not configured to access Client Db with name [${projectDbRef.name}] in region '${projectDbRef.region}'")
      case Some(db) => db
    }
  }
}

object GlobalDatabaseManager {
  val singleConfigRoot    = "clientDatabases"
  val allConfigRoot       = "allClientDatabases"
  val awsRegionConfigProp = "awsRegion"

  def initializeForSingleRegion(config: Config): GlobalDatabaseManager = {
    import scala.collection.JavaConversions._

    config.resolve()
    val currentRegion = Region.withName(config.getString(awsRegionConfigProp))

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

      ProjectDatabaseRef(currentRegion, dbName) -> dbs
    }

    GlobalDatabaseManager(currentRegion = currentRegion, databases = databasesMap.toMap)
  }

  def initializeForMultipleRegions(config: Config): GlobalDatabaseManager = {
    import scala.collection.JavaConversions._

    val currentRegion = Region.withName(config.getString(awsRegionConfigProp))

    val databasesMap = for {
      (regionName, regionValue) <- config.getObject(allConfigRoot)
      (dbName, _)               <- regionValue.asInstanceOf[ConfigObject]
    } yield {
      val readOnlyPath    = s"$allConfigRoot.$regionName.$dbName.readonly"
      val masterDb        = Database.forConfig(s"$allConfigRoot.$regionName.$dbName.master", config)
      lazy val readOnlyDb = Database.forConfig(readOnlyPath, config)

      val dbs = Databases(
        master = masterDb,
        readOnly = if (config.hasPath(readOnlyPath)) readOnlyDb else masterDb
      )

      ProjectDatabaseRef(Region.withName(regionName), dbName) -> dbs
    }

    GlobalDatabaseManager(currentRegion = currentRegion, databases = databasesMap.toMap)
  }
}
