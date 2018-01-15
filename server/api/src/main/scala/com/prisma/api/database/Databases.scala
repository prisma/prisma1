package com.prisma.api.database

import com.typesafe.config.Config
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

case class Databases(master: DatabaseDef, readOnly: DatabaseDef)

object Databases {
  private lazy val dbDriver = new org.mariadb.jdbc.Driver
  private val configRoot    = "clientDatabases"

  def initialize(config: Config): Databases = {
    import scala.collection.JavaConverters._
    config.resolve()

    val databasesMap = for {
      dbName <- asScalaSet(config.getObject(configRoot).keySet())
    } yield {
      val readOnlyPath    = s"$configRoot.$dbName.readonly"
      val masterDb        = Database.forConfig(s"$configRoot.$dbName.master", config, driver = dbDriver)
      lazy val readOnlyDb = Database.forConfig(readOnlyPath, config, driver = dbDriver)

      val dbs = Databases(
        master = masterDb,
        readOnly = if (config.hasPath(readOnlyPath)) readOnlyDb else masterDb
      )

      dbName -> dbs
    }

    databasesMap.head._2
  }
}
