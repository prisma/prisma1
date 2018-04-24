package com.prisma.deploy.connector.postgresql

import com.prisma.config.ConfigLoader
import com.prisma.deploy.connector.postgresql.database.InternalDatabaseSchema
import com.prisma.utils.await.AwaitUtils
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.PostgresProfile.api._

class InternalTestDatabase extends AwaitUtils {

  val config = ConfigLoader.load()

  val databaseDefs         = InternalDatabaseDefs(config.databases.head.copy(pooled = false))
  val internalDatabaseRoot = databaseDefs.internalDatabaseRoot
  val internalDatabase     = databaseDefs.internalDatabase

  def createInternalDatabaseSchema() = internalDatabaseRoot.run(InternalDatabaseSchema.createSchemaActions(recreate = true)).await(10)

  def truncateTables(): Unit = {
    val schemas = internalDatabase.run(getTables).await()
    internalDatabase.run(dangerouslyTruncateTables(schemas)).await()
  }

  //todo set up prisma schema and only wipe within that schema
  private def dangerouslyTruncateTables(tableNames: Vector[String]): DBIOAction[Unit, NoStream, Effect] = {
    DBIO.seq(tableNames.map(name => sqlu"""TRUNCATE TABLE "#$name" cascade"""): _*)
  }

  private def getTables = {
    sql"""SELECT table_name
          FROM information_schema.tables
          WHERE table_schema = 'public'
          AND table_type = 'BASE TABLE';""".as[String]
  }

  def run[R](a: DBIOAction[R, NoStream, Nothing]) = internalDatabase.run(a).await()

  def shutdown() = {
    internalDatabaseRoot.close()
    internalDatabase.close()
  }
}
