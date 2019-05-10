package com.prisma.api.connector.sqlite

import java.sql.Driver

import com.prisma.config.DatabaseConfig
import com.prisma.connector.shared.jdbc.{Databases, SlickDatabase}
import com.typesafe.config.{Config, ConfigFactory}
import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._

object SQLiteDatabasesFactory {

  def initialize(dbConfig: DatabaseConfig, driver: Driver): Databases = {
    val masterDb                     = Database.forDriver(driver, "jdbc:sqlite:database.db")
    val slickDatabase: SlickDatabase = SlickDatabase(SQLiteProfile, masterDb)

    Databases(primary = slickDatabase, replica = slickDatabase)
  }
}
